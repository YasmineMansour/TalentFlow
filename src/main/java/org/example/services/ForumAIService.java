package org.example.services;

import org.example.model.Post;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service d'assistant IA pour le forum TalentFlow.
 * Utilise les posts du forum comme contexte pour répondre aux questions.
 */
public class ForumAIService {

    private final PostService postService = new PostService();

    /**
     * Pose une question à l'IA en utilisant les posts du forum comme contexte.
     */
    public String askForumAI(String question) throws SQLException {
        // 1. Récupérer tous les posts
        List<Post> posts = postService.afficher();

        if (posts.isEmpty()) {
            return "Il n'y a actuellement aucun post dans le forum.";
        }

        // 2. Construire le contexte à partir des posts
        String context = posts.stream()
                .map(p -> "Titre: " + p.getTitle() +
                        "\nAuteur: " + p.getAuthorName() +
                        "\nContenu: " + p.getContent() +
                        "\nVotes: " + p.getUpvotes() +
                        "\n---")
                .collect(Collectors.joining("\n"));

        // 3. Prompt professionnel
        String prompt =
                "Tu es un assistant professionnel du forum TalentFlow.\n" +
                "Réponds de manière structurée, claire et formelle.\n" +
                "Utilise des puces quand c'est utile.\n" +
                "N'invente PAS d'informations.\n" +
                "Si ce n'est pas mentionné dans le forum, dis : 'Cette information n'a pas été discutée dans le forum.'\n\n" +
                "Données du Forum:\n" + context + "\n\n" +
                "Question de l'utilisateur:\n" + question + "\n\n" +
                "Réponse professionnelle:";

        // 4. Appel à l'IA
        return ForumCloudService.askAI(prompt);
    }
}
