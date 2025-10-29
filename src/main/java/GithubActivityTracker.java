import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper;

public class GithubActivityTracker {

    private static final String API_URL = "https://api.github.com/users/%s/events";

    public static void main(String[] args) {
        // 1. Manejar Argumentos CLI
        if (args.length != 1) {
            System.out.println("Uso: java -jar github-activity.jar <nombre_de_usuario_github>");
            return;
        }
        String username = args[0];
        
        try {
            // 2. Obtener la Actividad de la API
            String jsonResponse = fetchUserActivity(username);
            
            // 3. Procesar y Mostrar la Actividad
            displayActivity(jsonResponse);
            
        } catch (IOException | InterruptedException e) {
            System.err.println("Error de conexión o lectura de datos: " + e.getMessage());
        } catch (Exception e) {
            // Manejo de errores de la API (ej: usuario no encontrado)
            System.err.println("Error al procesar la actividad: " + e.getMessage());
        }
    }

    /**
     * Realiza la solicitud HTTP a la API de GitHub.
     */
    private static String fetchUserActivity(String username) throws IOException, InterruptedException {
        String url = String.format(API_URL, username);
        
        // El cliente de Java 11+
        HttpClient client = HttpClient.newHttpClient();
        
        // Construcción de la solicitud GET
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                // **Opcional/Recomendado:** Añadir un token para aumentar el límite de tasa
                // .header("Authorization", "Bearer TU_GITHUB_PAT")
                .GET()
                .build();

        // Enviar la solicitud y recibir la respuesta
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 404) {
            throw new RuntimeException("Error: El usuario '" + username + "' no fue encontrado.");
        }
        if (response.statusCode() != 200) {
            throw new RuntimeException("Error en la API de GitHub. Código de estado: " + response.statusCode());
        }

        return response.body();
    }

    /**
     * Usa Jackson para mapear el JSON y mostrar la actividad.
     */
    private static void displayActivity(String jsonResponse) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        
        // Mapea el arreglo JSON a una lista de objetos GitHubEvent
        List<GithubEvent> events = mapper.readValue(
            jsonResponse, 
            mapper.getTypeFactory().constructCollectionType(List.class, GithubEvent.class)
        );

        System.out.println("\n--- Actividad Reciente del Usuario ---\n");
        
        // Itera y formatea la salida como se pide en el requerimiento
        for (GithubEvent event : events) {
            String repoName = (event.getRepo() != null) ? event.getRepo().getName() : "Repositorio desconocido";
            String activityDescription = formatEvent(event);
            
            if (!activityDescription.isEmpty()) {
                 System.out.println("  - " + activityDescription + " en " + repoName);
            }
        }
        System.out.println("\n--------------------------------------");
    }

    /**
     * Convierte el tipo de evento y el payload en una descripción legible.
     */
    private static String formatEvent(GithubEvent event) {
        switch (event.getType()) {
            case "PushEvent":
                // Intenta obtener el número de commits desde el payload
                int commitCount = (event.getPayload().get("commits") instanceof List) 
                                ? ((List<?>) event.getPayload().get("commits")).size() 
                                : 0;
                return "Pushed " + commitCount + " commits";
            case "CreateEvent":
                // Obtener el tipo de objeto creado (ej: branch, tag, repository)
                String refType = (String) event.getPayload().get("ref_type");
                return "Created a new " + refType;
            case "IssuesEvent":
                String action = (String) event.getPayload().get("action");
                return "Opened a new issue (Action: " + action + ")";
            case "PullRequestEvent":
                String prAction = (String) event.getPayload().get("action");
                return "A pull request was " + prAction;
            case "WatchEvent":
                return "Starred (Watched)";
            default:
                // Ignorar o dar un mensaje genérico para otros tipos de evento
                return ""; 
        }
    }
}