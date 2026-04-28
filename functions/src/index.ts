import { onDocumentCreated } from "firebase-functions/v2/firestore";
import { initializeApp } from "firebase-admin/app";
import { getFirestore } from "firebase-admin/firestore";
import { getMessaging, MulticastMessage } from "firebase-admin/messaging";

initializeApp();

/**
 * À chaque nouvelle dépêche, on envoie une notification FCM à l'agent
 * destinataire. Si la dépêche est marquée "urgent", on envoie un payload
 * data-only haute priorité pour que l'app puisse réveiller le téléphone
 * et lancer l'écran de Transmission Urgente. Sinon, on envoie une notif
 * standard que le système affichera même si l'app est tuée.
 */
export const onMessageCreated = onDocumentCreated(
  {
    document: "messages/{messageId}",
    region: "europe-west1",
  },
  async (event) => {
    const data = event.data?.data();
    if (!data) return;

    const toUid: string = data.toUid;
    const fromCode: string = data.fromCode ?? "Agent inconnu";
    const rawBody: string = data.body ?? "";
    const urgent: boolean = data.urgent === true;
    const messageId: string = event.params.messageId;
    const attachmentType: string | undefined = data.attachmentType;

    const body: string = (() => {
      if (attachmentType === "image")
        return rawBody.length > 0 ? `📷 ${rawBody}` : "📷 Plaque photographique";
      if (attachmentType === "audio") return "🎙 Cylindre phonographique";
      return rawBody;
    })();

    if (!toUid) {
      console.warn("Dépêche sans destinataire, abandon", messageId);
      return;
    }

    const recipientRef = getFirestore().doc(`agents/${toUid}`);
    const recipientSnap = await recipientRef.get();
    const tokens: string[] = (recipientSnap.get("fcmTokens") as string[]) ?? [];
    if (tokens.length === 0) {
      console.log(`Aucun jeton FCM pour ${toUid}, on saute.`);
      return;
    }

    const message: MulticastMessage = {
      tokens,
      data: {
        type: urgent ? "urgent" : "normal",
        fromUid: data.fromUid ?? "",
        fromCode,
        toUid,
        body,
        messageId,
      },
      android: {
        priority: "high",
        // Pour les dépêches urgentes : payload data-only (pas de bloc
        // "notification") pour que onMessageReceived soit appelé même
        // si l'app est en arrière-plan ou tuée — l'app prendra le
        // relais pour afficher son propre écran plein-écran.
        // Pour les dépêches normales : on inclut un bloc notification
        // pour que le système l'affiche directement.
        ...(urgent
          ? {}
          : {
              notification: {
                channelId: "depeches",
                title: `Dépêche de ${fromCode}`,
                body,
                sound: "default",
              },
            }),
      },
    };

    const response = await getMessaging().sendEachForMulticast(message);

    // Nettoyer les jetons devenus invalides (app désinstallée, jeton expiré)
    const stale: string[] = [];
    response.responses.forEach((r, idx) => {
      if (!r.success) {
        const code = r.error?.code;
        if (
          code === "messaging/registration-token-not-registered" ||
          code === "messaging/invalid-registration-token" ||
          code === "messaging/invalid-argument"
        ) {
          stale.push(tokens[idx]);
        } else {
          console.warn(`Échec FCM pour ${tokens[idx]}:`, code, r.error?.message);
        }
      }
    });
    if (stale.length > 0) {
      const valides = tokens.filter((t) => !stale.includes(t));
      await recipientRef.update({ fcmTokens: valides });
      console.log(`Jetons obsolètes purgés pour ${toUid}: ${stale.length}`);
    }

    console.log(
      `Dépêche ${messageId} → ${toUid} : ${response.successCount}/${tokens.length} OK${
        urgent ? " (URGENT)" : ""
      }`
    );
  }
);
