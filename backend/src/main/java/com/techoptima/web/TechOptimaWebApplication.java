package com.techoptima.web;

import com.techoptima.model.Application;
import com.techoptima.model.Criticality;
import com.techoptima.model.Department;
import com.techoptima.model.TransformationBudget;
import com.techoptima.repository.ApplicationRepository;
import com.techoptima.repository.TransformationBudgetRepository;
import com.techoptima.service.OptimizationResult;
import com.techoptima.service.PortfolioOptimizationService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;

public final class TechOptimaWebApplication {

    private static final int PORT = configuredPort();
    private static final Path FRONTEND_ROOT = resolveFrontendRoot();
    private static final BigDecimal MAX_MONEY =
            new BigDecimal("9999999999999.99");
    private static final int MAX_FORM_BODY_BYTES = 16 * 1024;

    private final ApplicationRepository applicationRepository =
            new ApplicationRepository();

    private final TransformationBudgetRepository budgetRepository =
            new TransformationBudgetRepository();

    private final PortfolioOptimizationService optimizationService =
            new PortfolioOptimizationService();

    private TechOptimaWebApplication() {
    }

    public static void main(String[] args) throws Exception {

        TechOptimaWebApplication application =
                new TechOptimaWebApplication();

        HttpServer server =
                HttpServer.create(
                        new InetSocketAddress(PORT),
                        0
                );

        server.createContext(
                "/",
                application::handleIndex
        );

        server.createContext(
                "/css",
                exchange -> serveStaticAsset(exchange, "/css/")
        );

        server.createContext(
                "/js",
                exchange -> serveStaticAsset(exchange, "/js/")
        );

        server.createContext(
                "/budget",
                application::handleBudget
        );

        server.createContext(
                "/application",
                application::handleApplication
        );

        
        server.createContext(
                "/application/edit",
                application::handleApplicationEdit
        );

        server.createContext(
                "/application/update",
                application::handleApplicationUpdate
        );

        server.createContext(
                "/application/delete",
                application::handleApplicationDelete
        );

        server.createContext(
                "/dependency",
                application::handleDependency
        );

        server.createContext(
                "/optimize",
                application::handleOptimize
        );

        server.setExecutor(
                Executors.newCachedThreadPool()
        );

        server.start();

        System.out.println(
                "TechOptima web application started."
        );

        System.out.println(
                "Open: http://localhost:" + PORT
        );
    }

    private static int configuredPort() {

        String configuredPort = System.getenv("TECHOPTIMA_PORT");

        if (configuredPort == null || configuredPort.isBlank()) {
            return 8080;
        }

        try {
            int port = Integer.parseInt(configuredPort);

            if (port > 0 && port <= 65535) {
                return port;
            }
        } catch (NumberFormatException ignored) {
            // Fall through to the local-development default.
        }

        return 8080;
    }

    private static Path resolveFrontendRoot() {

        String configuredPath = System.getenv("TECHOPTIMA_FRONTEND_DIR");

        if (configuredPath != null && !configuredPath.isBlank()) {
            return Paths.get(configuredPath).toAbsolutePath().normalize();
        }

        Path workingDirectory = Paths.get("").toAbsolutePath().normalize();
        Path siblingFrontend =
                workingDirectory.resolveSibling("frontend");

        if (Files.isDirectory(siblingFrontend)) {
            return siblingFrontend;
        }

        return workingDirectory.resolve("frontend").normalize();
    }

    private void handleIndex(
            HttpExchange exchange)
            throws IOException {

        if (!"/".equals(exchange.getRequestURI().getPath())) {
            sendNotFound(exchange, "Endpoint not found.");
            return;
        }

        if (!"GET".equalsIgnoreCase(
                exchange.getRequestMethod())) {

            sendMethodNotAllowed(exchange);
            return;
        }

        try {

            Path file = FRONTEND_ROOT.resolve("index.html");

            String html =
                    Files.readString(
                            file,
                            StandardCharsets.UTF_8
                    );

            StringBuilder databaseTable =
                    new StringBuilder();

            renderDatabaseData(databaseTable);

            html = html.replace(
                    "<div id=\"database-table\"></div>",
                    databaseTable.toString()
            );

            sendHtml(
                    exchange,
                    html
            );

        } catch (Exception exception) {

            sendRequestFailure(
                    exchange,
                    "Unable to load dashboard",
                    exception
            );
        }
    }
    private static void serveStaticAsset(
            HttpExchange exchange,
            String requestPrefix)
            throws IOException {

        String requestPath = exchange.getRequestURI().getPath();

        if (!requestPath.startsWith(requestPrefix)) {
            sendNotFound(exchange, "Static asset not found.");
            return;
        }

        serveFrontendFile(
                exchange,
                requestPrefix.substring(1, requestPrefix.length() - 1)
                        + java.io.File.separator
                        + requestPath.substring(requestPrefix.length())
                                .replace("/", java.io.File.separator)
        );
    }

    private static void serveFrontendFile(
            HttpExchange exchange,
            String fileName)
            throws IOException {

        if (!"GET".equalsIgnoreCase(
                exchange.getRequestMethod())) {

            sendMethodNotAllowed(exchange);
            return;
        }

        Path file = FRONTEND_ROOT.resolve(fileName).normalize();

        if (!file.startsWith(FRONTEND_ROOT)
                || !Files.exists(file)
                || Files.isDirectory(file)) {

            sendNotFound(exchange, "Static asset not found.");
            return;
        }

        byte[] body =
                Files.readAllBytes(file);

        String contentType = "text/plain; charset=UTF-8";

        if (fileName.endsWith(".html")) {
            contentType = "text/html; charset=UTF-8";
        } else if (fileName.endsWith(".css")) {
            contentType = "text/css; charset=UTF-8";
        } else if (fileName.endsWith(".js")) {
            contentType = "application/javascript; charset=UTF-8";
        }

        applySecurityHeaders(exchange);

        exchange.getResponseHeaders()
                .set("Content-Type", contentType);

        exchange.sendResponseHeaders(
                200,
                body.length
        );

        try (OutputStream output =
                     exchange.getResponseBody()) {

            output.write(body);
        }
    }

    private void handleBudget(
            HttpExchange exchange)
            throws IOException {

        if (!"POST".equalsIgnoreCase(
                exchange.getRequestMethod())) {

            sendMethodNotAllowed(exchange);
            return;
        }

        try {

            Map<String, String> form =
                    parseForm(exchange);

            BigDecimal amount = requireMoney(form, "amount");

            budgetRepository.replaceCurrent(
                    new TransformationBudget(amount)
            );

            redirectWithFeedback(
                    exchange,
                    "success",
                    "Budget saved successfully."
            );

        } catch (Exception exception) {

            sendRequestFailure(exchange, "Budget save failed", exception);
        }
    }

    private void handleApplication(
            HttpExchange exchange)
            throws IOException {

        if (!"POST".equalsIgnoreCase(
                exchange.getRequestMethod())) {

            sendMethodNotAllowed(exchange);
            return;
        }

        try {

            Map<String, String> form =
                    parseForm(exchange);

            Application application = parseApplication(form);

            if (applicationRepository.findById(
                    application.getApplicationId()) != null) {
                throw conflict(
                        "Application ID "
                                + application.getApplicationId()
                                + " already exists."
                );
            }

            validateDependencyTargets(
                    application.getDependencyApplicationIds()
            );

            applicationRepository.save(
                    application
            );

            redirectWithFeedback(
                    exchange,
                    "success",
                    "Application added successfully."
            );

        } catch (Exception exception) {

            sendRequestFailure(exchange, "Application save failed", exception);
        }
    }

    private void handleApplicationEdit(
            HttpExchange exchange)
            throws IOException {

        if (!"GET".equalsIgnoreCase(
                exchange.getRequestMethod())) {

            sendMethodNotAllowed(exchange);
            return;
        }

        try {

            Map<String, String> query =
                    parseQuery(
                            exchange.getRequestURI()
                                    .getRawQuery()
                    );

            long id = requirePositiveId(query, "id", "Application ID");

            Application application =
                    applicationRepository.findById(id);

            if (application == null) {
                throw notFound(
                        "Application ID " + id + " was not found."
                );
            }

            StringBuilder html =
                    new StringBuilder(8000);

            html.append(pageStart(
                    "Edit Application"
            ));

            String formattedDependencies = application
                    .getDependencyApplicationIds()
                    .stream()
                    .map(String::valueOf)
                    .collect(java.util.stream.Collectors.joining(", "));

            html.append("<section class='application-section'>")
                    .append("<div class='application-header'>")
                    .append("<div>")
                    .append("<span class='section-label'>APPLICATION MANAGEMENT</span>")
                    .append("<h2>Edit Application</h2>")
                    .append("<p>Update application parameters and dependencies. Fields are pre-filled with current values.</p>")
                    .append("</div>")
                    .append("</div>")
                    .append("<form class='application-form' method='post' action='/application/update'>")

                    .append("<div class='form-field'>")
                    .append("<label for='id'>Application ID</label>")
                    .append("<input id='id' name='id' type='number' value='")
                    .append(application.getApplicationId())
                    .append("' readonly style='background-color: #f7f7f7; color: #555555; cursor: not-allowed;' required>")
                    .append("</div>")

                    .append("<div class='form-field'>")
                    .append("<label for='name'>Application Name</label>")
                    .append("<input id='name' name='name' type='text' value='")
                    .append(escape(application.getApplicationName()))
                    .append("' placeholder='Application Name' required>")
                    .append("</div>")

                    .append("<div class='form-field'>")
                    .append("<label for='cost'>Modernization Cost</label>")
                    .append("<div class='input-prefix'>")
                    .append("<span>$</span>")
                    .append("<input id='cost' name='cost' type='number' step='0.01' min='0' value='")
                    .append(application.getModernizationCost().toPlainString())
                    .append("' placeholder='0.00' required>")
                    .append("</div>")
                    .append("</div>")

                    .append("<div class='form-field'>")
                    .append("<label for='benefit'>Business Benefit</label>")
                    .append("<div class='input-suffix'>")
                    .append("<input id='benefit' name='benefit' type='number' min='0' max='100' value='")
                    .append(application.getBusinessBenefit())
                    .append("' placeholder='85' required>")
                    .append("<span>%</span>")
                    .append("</div>")
                    .append("</div>")

                    .append("<div class='form-field'>")
                    .append("<label for='criticality'>Criticality</label>")
                    .append("<select id='criticality' name='criticality' required>");

            for (Criticality value : Criticality.values()) {
                html.append("<option value='")
                        .append(value.name())
                        .append("'");

                if (value == application.getCriticality()) {
                    html.append(" selected");
                }

                html.append(">")
                        .append(value.name())
                        .append("</option>");
            }

            html.append("</select>")
                    .append("</div>")

                    .append("<div class='form-field'>")
                    .append("<label for='department'>Department</label>")
                    .append("<select id='department' name='department' required>");

            for (Department value : Department.values()) {
                html.append("<option value='")
                        .append(value.name())
                        .append("'");

                if (value == application.getDepartment()) {
                    html.append(" selected");
                }

                html.append(">")
                        .append(escape(value.name().replace('_', ' ')))
                        .append("</option>");
            }

            html.append("</select>")
                    .append("</div>")

                    .append("<div class='form-field dependencies-field'>")
                    .append("<label for='dependencies'>Dependency IDs</label>")
                    .append("<input id='dependencies' name='dependencies' type='text' value='")
                    .append(escape(formattedDependencies))
                    .append("' placeholder='1001, 1002' aria-describedby='dependencies-help'>")
                    .append("<small id='dependencies-help'>Enter application IDs separated by commas</small>")
                    .append("</div>")

                    .append("<div class='form-action'>")
                    .append("<button type='submit' data-loading-label='Updating application…'>")
                    .append("<span>✓</span>")
                    .append("Update Application")
                    .append("</button>")
                    .append("</div>")

                    .append("</form>")
                    .append("<p style='margin-top: 28px;'><a class='button' href='/' style='text-decoration: none;'>← Back to Dashboard</a></p>")
                    .append("</section>");

            html.append(pageEnd());

            sendHtml(
                    exchange,
                    html.toString()
            );

        } catch (Exception exception) {

            sendRequestFailure(exchange, "Application edit failed", exception);
        }
    }

    private void handleApplicationUpdate(
            HttpExchange exchange)
            throws IOException {

        if (!"POST".equalsIgnoreCase(
                exchange.getRequestMethod())) {

            sendMethodNotAllowed(exchange);
            return;
        }

        try {

            Map<String, String> form =
                    parseForm(exchange);

            Application application = parseApplication(form);

            if (applicationRepository.findById(
                    application.getApplicationId()) == null) {
                throw notFound(
                        "Application ID "
                                + application.getApplicationId()
                                + " was not found."
                );
            }

            validateDependencyTargets(
                    application.getDependencyApplicationIds()
            );

            applicationRepository.update(
                    application
            );

            redirectWithFeedback(
                    exchange,
                    "success",
                    "Application updated successfully."
            );

        } catch (Exception exception) {

            sendRequestFailure(exchange, "Application update failed", exception);
        }
    }

    private void handleApplicationDelete(
            HttpExchange exchange)
            throws IOException {

        if (!"POST".equalsIgnoreCase(
                exchange.getRequestMethod())) {

            sendMethodNotAllowed(exchange);
            return;
        }

        try {

            Map<String, String> form =
                    parseForm(exchange);

            long id = requirePositiveId(form, "id", "Application ID");

            boolean deleted =
                    applicationRepository.deleteById(id);

            if (!deleted) {
                throw notFound(
                        "Application ID " + id + " was not found."
                );
            }

            redirectWithFeedback(
                    exchange,
                    "success",
                    "Application deleted successfully."
            );

        } catch (Exception exception) {

            sendRequestFailure(exchange, "Application delete failed", exception);
        }
    }
    private void handleDependency(
            HttpExchange exchange)
            throws IOException {

        if (!"POST".equalsIgnoreCase(
                exchange.getRequestMethod())) {

            sendMethodNotAllowed(exchange);
            return;
        }

        try {

            Map<String, String> form =
                    parseForm(exchange);

            long applicationId = requirePositiveId(
                    form,
                    "applicationId",
                    "Application ID"
            );

            long dependencyId = requirePositiveId(
                    form,
                    "dependencyId",
                    "Dependency application ID"
            );

            if (applicationId == dependencyId) {
                throw badRequest(
                        "An application cannot depend on itself."
                );
            }

            Application application =
                    applicationRepository.findById(
                            applicationId
                    );

            if (application == null) {
                throw notFound(
                        "Application ID "
                                + applicationId
                                + " was not found."
                );
            }

            if (applicationRepository.findById(dependencyId) == null) {
                throw notFound(
                        "Dependency application ID "
                                + dependencyId
                                + " was not found."
                );
            }

            List<Long> dependencies =
                    new ArrayList<>(
                            application
                                    .getDependencyApplicationIds()
                    );

            if (!dependencies.contains(
                    dependencyId)) {

                dependencies.add(dependencyId);
            }

            applicationRepository.update(
                    new Application(
                            application.getApplicationId(),
                            application.getApplicationName(),
                            application.getModernizationCost(),
                            application.getBusinessBenefit(),
                            application.getCriticality(),
                            application.getDepartment(),
                            dependencies
                    )
            );

            redirectWithFeedback(
                    exchange,
                    "success",
                    "Dependency saved successfully."
            );

        } catch (Exception exception) {

            sendRequestFailure(exchange, "Dependency save failed", exception);
        }
    }

    private void handleOptimize(
            HttpExchange exchange)
            throws IOException {

        if (!"GET".equalsIgnoreCase(
                exchange.getRequestMethod())) {

            sendMethodNotAllowed(exchange);
            return;
        }

        try {

            OptimizationResult result =
                    optimizationService.optimize();

            StringBuilder html =
                    new StringBuilder(10000);

            html.append(pageStart(
                    "Optimization Result"
            ));

            html.append("<h1>Optimization Result</h1>");

            html.append("<section>");
            html.append(
                    "<h2>Budget</h2>"
            );

            html.append("<p><strong>")
                    .append(result.getBudget()
                            .getBudgetAmount())
                    .append("</strong></p>");

            html.append("</section>");

            html.append("<section>");
            html.append(
                    "<h2>Selected Applications</h2>"
            );

            html.append("<table>");
            html.append(
                    "<tr><th>Application</th>"
                    + "<th>Cost</th>"
                    + "<th>Benefit</th>"
                    + "<th>Criticality</th></tr>"
            );

            for (Application application :
                    result.getKnapsackResult()
                            .getSelectedApplications()) {

                html.append("<tr>");

                html.append("<td>")
                        .append(escape(
                                application.getApplicationName()
                        ))
                        .append("</td>");

                html.append("<td>")
                        .append(application.getModernizationCost())
                        .append("</td>");

                html.append("<td>")
                        .append(application.getBusinessBenefit())
                        .append("</td>");

                html.append("<td>")
                        .append(application.getCriticality())
                        .append("</td>");

                html.append("</tr>");
            }

            html.append("</table>");

            html.append(
                    "<p>Total Cost: <strong>"
                            + result.getKnapsackResult()
                                    .getTotalCost()
                            + "</strong></p>"
            );

            html.append(
                    "<p>Total Benefit: <strong>"
                            + result.getKnapsackResult()
                                    .getTotalBusinessBenefit()
                            + "</strong></p>"
            );

            html.append("</section>");

            html.append("<section>");
            html.append("<div class='result-grid'>");
            html.append("<div class='result-card'>");

            html.append(
                    "<h2>Dependency Validation</h2>"
            );

            if (result
                    .getDependencyValidationResult()
                    .isValid()) {

                html.append(
                        "<p class='success'>PASSED</p>"
                );

            } else {

                html.append(
                        "<p class='error'>FAILED</p>"
                );

                html.append(
                        "<p>Missing dependencies: "
                                + result
                                        .getDependencyValidationResult()
                                        .getMissingDependenciesByApplication()
                                + "</p>"
                );
            }

            html.append("</div>");

            if (result.isSuccessful()) {

                html.append("<div class='result-card'>");

                html.append(
                        "<h2>Final Upgrade Order</h2>"
                );

                html.append("<ol>");

                for (Application application :
                        result.getTopologicalSortResult()
                                .getOrderedApplications()) {

                    html.append("<li>")
                            .append(escape(
                                    application
                                            .getApplicationName()
                            ))
                            .append(" - ")
                            .append(application
                                    .getCriticality())
                            .append("</li>");
                }

                html.append("</ol>");

                html.append(
                        "<p class='success'>"
                                + "FINAL RECOMMENDATION: READY"
                                + "</p>"
                );

                html.append("</div>");
                html.append("</div>");

            } else {

                html.append(
                        "<p class='error'>"
                                + "Final recommendation unavailable."
                                + "</p>"
                );
            }

            html.append(
                    "<a class='button' href='/'>"
                            + "Back to Dashboard"
                            + "</a>"
            );

            html.append(pageEnd());

            sendHtml(
                    exchange,
                    html.toString()
            );

        } catch (Exception exception) {

            sendRequestFailure(exchange, "Optimization failed", exception);
        }
    }

    private void renderDatabaseData(
            StringBuilder html)
            throws Exception {

        TransformationBudget budget =
                budgetRepository.findLatest();

        html.append(
                "<p>Current Budget: <strong>"
                        + (
                        budget == null
                                ? "Not set"
                                : budget.getBudgetAmount()
                        )
                        + "</strong></p>"
        );

        List<Application> applications =
                applicationRepository.findAll();

        html.append("<div id='database-table'><table>");

        html.append(
                "<tr>"
                        + "<th>ID</th>"
                        + "<th>Name</th>"
                        + "<th>Cost</th>"
                        + "<th>Benefit</th>"
                        + "<th>Criticality</th>"
                        + "<th>Department</th>"
                        + "<th>Dependencies</th>"
                        + "<th>Actions</th>"
                        + "</tr>"
        );

        if (applications.isEmpty()) {
            html.append(
                    "<tr class='empty-state'><td colspan='8'>"
                            + "No applications have been added yet."
                            + "</td></tr>"
            );
        }

        for (Application application :
                applications) {

            html.append("<tr>");

            html.append("<td>")
                    .append(application.getApplicationId())
                    .append("</td>");

            html.append("<td>")
                    .append(escape(
                            application.getApplicationName()
                    ))
                    .append("</td>");

            html.append("<td>")
                    .append(application.getModernizationCost())
                    .append("</td>");

            html.append("<td>")
                    .append(application.getBusinessBenefit())
                    .append("</td>");

            html.append("<td>")
                    .append(application.getCriticality())
                    .append("</td>");

            html.append("<td>")
                    .append(application.getDepartment())
                    .append("</td>");

            html.append("<td>")
                    .append(application
                            .getDependencyApplicationIds())
                    .append("</td>");

            html.append("<td>");

            html.append(
                    "<a href='/application/edit?id="
                            + application.getApplicationId()
                            + "'>Edit</a>"
            );

            html.append(" | ");

            html.append(
                    "<form method='POST' action='/application/delete' "
                            + "style='display:inline;' "
                            + "onsubmit=\"return confirm('Delete this application?');\">"
                            + "<input type='hidden' name='id' value='"
                            + application.getApplicationId()
                            + "'>"
                            + "<button type='submit'>Delete</button>"
                            + "</form>"
            );
            html.append("</td>");

            html.append("</tr>");
        }

        html.append("</table></div>");
    }

    private Application parseApplication(
            Map<String, String> form) {

        long id = requirePositiveId(form, "id", "Application ID");
        String name = requireText(
                form,
                "name",
                "Application name",
                150
        );
        BigDecimal cost = requireMoney(form, "cost");
        int benefit = requireIntegerInRange(
                form,
                "benefit",
                "Business benefit",
                0,
                100
        );
        Criticality criticality = requireEnum(
                form,
                "criticality",
                "Criticality",
                Criticality.class
        );
        Department department = requireEnum(
                form,
                "department",
                "Department",
                Department.class
        );
        List<Long> dependencies = parseDependencies(
                form.get("dependencies")
        );

        if (dependencies.contains(id)) {
            throw badRequest("An application cannot depend on itself.");
        }

        return new Application(
                id,
                name,
                cost,
                benefit,
                criticality,
                department,
                dependencies
        );
    }

    private void validateDependencyTargets(
            List<Long> dependencyIds)
            throws SQLException {

        for (Long dependencyId : dependencyIds) {
            if (applicationRepository.findById(dependencyId) == null) {
                throw notFound(
                        "Dependency application ID "
                                + dependencyId
                                + " was not found."
                );
            }
        }
    }

    private static List<Long> parseDependencies(
            String value) {

        if (value == null
                || value.trim().isEmpty()) {

            return List.of();
        }

        String[] parts =
                value.split(",");

        List<Long> dependencies =
                new ArrayList<>(parts.length);
        Set<Long> uniqueDependencies = new HashSet<>();

        for (String part : parts) {

            String trimmed =
                    part.trim();

            if (trimmed.isEmpty()) {
                throw badRequest(
                        "Dependency IDs must be comma-separated positive numbers."
                );
            }

            long dependencyId = parsePositiveLong(
                    trimmed,
                    "Dependency application ID"
            );

            if (!uniqueDependencies.add(dependencyId)) {
                throw badRequest(
                        "Dependency IDs must not contain duplicates."
                );
            }

            dependencies.add(dependencyId);
        }

        return dependencies;
    }

    private static Map<String, String> parseQuery(
            String query) {

        return parseUrlEncoded(query);
    }

    private static Map<String, String> parseForm(
            HttpExchange exchange)
            throws IOException {

        String contentType = exchange.getRequestHeaders()
                .getFirst("Content-Type");

        if (contentType == null
                || !contentType.toLowerCase()
                .startsWith("application/x-www-form-urlencoded")) {
            throw unsupportedMediaType(
                    "This endpoint accepts application/x-www-form-urlencoded data only."
            );
        }

        return parseUrlEncoded(readFormBody(exchange));
    }

    private static String readFormBody(
            HttpExchange exchange)
            throws IOException {

        String declaredLength = exchange.getRequestHeaders()
                .getFirst("Content-Length");

        if (declaredLength != null) {
            try {
                long length = Long.parseLong(declaredLength);

                if (length < 0) {
                    throw badRequest("Request body length is invalid.");
                }

                if (length > MAX_FORM_BODY_BYTES) {
                    throw payloadTooLarge(
                            "Request data must not exceed "
                                    + MAX_FORM_BODY_BYTES
                                    + " bytes."
                    );
                }
            } catch (NumberFormatException exception) {
                throw badRequest("Request body length is invalid.");
            }
        }

        try (InputStream input = exchange.getRequestBody();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[1024];
            int bytesRead;

            while ((bytesRead = input.read(buffer)) != -1) {
                if (output.size() + bytesRead > MAX_FORM_BODY_BYTES) {
                    throw payloadTooLarge(
                            "Request data must not exceed "
                                    + MAX_FORM_BODY_BYTES
                                    + " bytes."
                    );
                }

                output.write(buffer, 0, bytesRead);
            }

            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private static Map<String, String> parseUrlEncoded(
            String value) {

        Map<String, String> values =
                new HashMap<>();

        if (value == null || value.isEmpty()) {

            return values;
        }

        for (String pair :
                value.split("&")) {

            String[] parts =
                    pair.split(
                            "=",
                            2
                    );

            String key =
                    decodeFormComponent(parts[0]);

            String decodedValue =
                    parts.length > 1
                            ? decodeFormComponent(parts[1])
                            : "";

            values.put(
                    key,
                    decodedValue
            );
        }

        return values;
    }

    private static String decodeFormComponent(
            String value) {

        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException exception) {
            throw badRequest("Form data is malformed.");
        }
    }

    private static long requirePositiveId(
            Map<String, String> form,
            String key,
            String label) {

        return parsePositiveLong(require(form, key), label);
    }

    private static long parsePositiveLong(
            String value,
            String label) {

        try {
            long parsed = Long.parseLong(value);

            if (parsed <= 0) {
                throw badRequest(label + " must be greater than 0.");
            }

            return parsed;
        } catch (NumberFormatException exception) {
            throw badRequest(label + " must be a whole number.");
        }
    }

    private static String requireText(
            Map<String, String> form,
            String key,
            String label,
            int maximumLength) {

        String value = require(form, key);

        if (value.length() > maximumLength) {
            throw badRequest(
                    label + " must be at most "
                            + maximumLength
                            + " characters."
            );
        }

        return value;
    }

    private static BigDecimal requireMoney(
            Map<String, String> form,
            String key) {

        String label = key.equals("amount")
                ? "Budget amount"
                : "Modernization cost";

        try {
            BigDecimal value = new BigDecimal(require(form, key));

            if (value.compareTo(BigDecimal.ZERO) < 0) {
                throw badRequest(label + " cannot be negative.");
            }

            if (value.compareTo(MAX_MONEY) > 0) {
                throw badRequest(
                        label + " must not exceed "
                                + MAX_MONEY.toPlainString()
                                + "."
                );
            }

            if (value.scale() > 2) {
                throw badRequest(
                        label + " can contain at most two decimal places."
                );
            }

            return value;
        } catch (NumberFormatException exception) {
            throw badRequest(label + " must be a valid number.");
        }
    }

    private static int requireIntegerInRange(
            Map<String, String> form,
            String key,
            String label,
            int minimum,
            int maximum) {

        try {
            int value = Integer.parseInt(require(form, key));

            if (value < minimum || value > maximum) {
                throw badRequest(
                        label + " must be between "
                                + minimum
                                + " and "
                                + maximum
                                + "."
                );
            }

            return value;
        } catch (NumberFormatException exception) {
            throw badRequest(label + " must be a whole number.");
        }
    }

    private static <T extends Enum<T>> T requireEnum(
            Map<String, String> form,
            String key,
            String label,
            Class<T> enumType) {

        String value = require(form, key);

        try {
            return Enum.valueOf(enumType, value);
        } catch (IllegalArgumentException exception) {
            throw badRequest(label + " is not a supported value.");
        }
    }

    private static String require(
            Map<String, String> form,
            String key) {

        String value =
                form.get(key);

        if (value == null
                || value.trim().isEmpty()
                || "null".equalsIgnoreCase(value.trim())) {

            throw badRequest(
                    key + " is required."
            );
        }

        return value.trim();
    }

    private static String escape(
            String value) {

        if (value == null) {
            return "";
        }

        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private static String pageStart(
            String title) {

        return "<!DOCTYPE html>"
                + "<html lang='en'>"
                + "<head>"
                + "<meta charset='UTF-8'>"
                + "<meta name='viewport' "
                + "content='width=device-width, initial-scale=1'>"
                + "<title>"
                + escape(title)
                + "</title>"
                + "<link rel='stylesheet' href='/css/style.css?v=5'>"
                + "<script src='/js/app.js' defer></script>"
                + "</head>"
                + "<body class='generated-page'>"
                + "<main class='container generated-page__content'>";
    }

    private static String pageEnd() {
        return "</main></body></html>";
    }

    private static void redirect(
            HttpExchange exchange,
            String location)
            throws IOException {

        exchange.getResponseHeaders()
                .set(
                        "Location",
                        location
                );

        exchange.sendResponseHeaders(
                303,
                -1
        );

        exchange.close();
    }

    private static void redirectWithFeedback(
            HttpExchange exchange,
            String type,
            String message)
            throws IOException {

        redirect(
                exchange,
                "/?feedback="
                        + URLEncoder.encode(
                                type,
                                StandardCharsets.UTF_8
                        )
                        + "&message="
                        + URLEncoder.encode(
                                message,
                                StandardCharsets.UTF_8
                        )
        );
    }

    private static void applySecurityHeaders(
            HttpExchange exchange) {

        exchange.getResponseHeaders()
                .set("X-Content-Type-Options", "nosniff");
        exchange.getResponseHeaders()
                .set("X-Frame-Options", "DENY");
        exchange.getResponseHeaders()
                .set("Referrer-Policy", "strict-origin-when-cross-origin");
        exchange.getResponseHeaders()
                .set("Content-Security-Policy", "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; img-src 'self' data:;");
    }

    private static void sendHtml(
            HttpExchange exchange,
            String html)
            throws IOException {

        sendHtml(exchange, html, 200);
    }

    private static void sendHtml(
            HttpExchange exchange,
            String html,
            int statusCode)
            throws IOException {

        applySecurityHeaders(exchange);

        byte[] body =
                html.getBytes(
                        StandardCharsets.UTF_8
                );

        exchange.getResponseHeaders()
                .set(
                        "Content-Type",
                        "text/html; charset=UTF-8"
                );

        exchange.sendResponseHeaders(
                statusCode,
                body.length
        );

        try (OutputStream output =
                     exchange.getResponseBody()) {

            output.write(body);
        }
    }

    private static void sendHtmlError(
            HttpExchange exchange,
            int statusCode,
            String message)
            throws IOException {

        String html =
                pageStart("TechOptima Error")
                        + "<div id='notification-region' class='notification-region'>"
                        + "<div class='notification notification--error' role='alert'>"
                        + "<strong>Error:</strong><span data-server-message>"
                        + escape(message)
                        + "</span></div></div>"
                        + "<h1>Error</h1>"
                        + "<p class='error'>Please correct the issue and try again.</p>"
                        + "<a class='button' href='/'>"
                        + "Back"
                        + "</a>"
                        + pageEnd();

        sendHtml(exchange, html, statusCode);
    }

    private static void sendMethodNotAllowed(
            HttpExchange exchange)
            throws IOException {

        sendHtmlError(
                exchange,
                405,
                "This endpoint does not support the supplied HTTP method."
        );
    }

    private static void sendNotFound(
            HttpExchange exchange,
            String message)
            throws IOException {

        sendHtmlError(exchange, 404, message);
    }

    private static void sendRequestFailure(
            HttpExchange exchange,
            String operation,
            Exception exception)
            throws IOException {

        if (exception instanceof HttpRequestException) {
            HttpRequestException requestException =
                    (HttpRequestException) exception;

            sendHtmlError(
                    exchange,
                    requestException.getStatusCode(),
                    operation + ": " + requestException.getMessage()
            );
            return;
        }

        if (exception instanceof IllegalStateException) {
            sendHtmlError(
                    exchange,
                    422,
                    operation + ": " + exception.getMessage()
            );
            return;
        }

        if (exception instanceof IllegalArgumentException) {
            sendHtmlError(
                    exchange,
                    400,
                    operation + ": " + exception.getMessage()
            );
            return;
        }

        if (exception instanceof SQLException
                && isDataConflict((SQLException) exception)) {
            sendHtmlError(
                    exchange,
                    409,
                    operation + ": The request conflicts with current data."
            );
            return;
        }

        if (exception instanceof SQLException) {
            sendHtmlError(
                    exchange,
                    503,
                    operation + ": The database is unavailable. Please try again later."
            );
            return;
        }

        sendHtmlError(
                exchange,
                500,
                operation + ": An unexpected server error occurred."
        );
    }

    private static boolean isDataConflict(
            SQLException exception) {

        String sqlState = exception.getSQLState();

        return sqlState != null && sqlState.startsWith("23");
    }

    private static HttpRequestException badRequest(
            String message) {

        return new HttpRequestException(400, message);
    }

    private static HttpRequestException notFound(
            String message) {

        return new HttpRequestException(404, message);
    }

    private static HttpRequestException conflict(
            String message) {

        return new HttpRequestException(409, message);
    }

    private static HttpRequestException payloadTooLarge(
            String message) {

        return new HttpRequestException(413, message);
    }

    private static HttpRequestException unsupportedMediaType(
            String message) {

        return new HttpRequestException(415, message);
    }

    private static final class HttpRequestException
            extends RuntimeException {

        private final int statusCode;

        private HttpRequestException(
                int statusCode,
                String message) {

            super(message);
            this.statusCode = statusCode;
        }

        private int getStatusCode() {
            return statusCode;
        }
    }
}
