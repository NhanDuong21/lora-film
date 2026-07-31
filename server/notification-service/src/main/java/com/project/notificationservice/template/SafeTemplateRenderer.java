package com.project.notificationservice.template;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Options;
import com.github.jknack.handlebars.Template;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import com.project.notificationservice.exception.NotificationException;
import com.project.notificationservice.template.TemplateRegistry.RenderedTemplate;
import com.project.notificationservice.template.TemplateRegistry.TemplateDocument;
import com.project.notificationservice.template.TemplateRegistry.TemplateValidationResult;
import com.project.notificationservice.template.TemplateRegistry.VariableDefinition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class SafeTemplateRenderer {

    private static final Pattern EXPRESSION = Pattern.compile("\\{\\{\\s*([^{}]+?)\\s*}}");
    private static final Set<String> HELPERS = Set.of(
            "formatDate", "formatTime", "formatDateTime", "formatCurrency", "join",
            "uppercase", "lowercase", "maskEmail", "maskPhone", "defaultValue");
    private static final Set<String> LITERALS = Set.of("true", "false", "null", "this", ".");
    private static final PolicyFactory HTML_POLICY = new HtmlPolicyBuilder()
            .allowElements("a", "p", "div", "span", "table", "thead", "tbody", "tr", "th", "td",
                    "h1", "h2", "h3", "h4", "strong", "em", "br", "ul", "ol", "li", "img", "hr")
            .allowAttributes("href", "title").onElements("a")
            .allowAttributes("src", "alt", "width", "height").onElements("img")
            .allowAttributes("class", "style").globally()
            .allowStandardUrlProtocols()
            .toFactory();

    private final Handlebars handlebars;
    private final int maxRenderedBytes;
    private final int maxSubjectLength;
    private final int maxSmsLength;

    public SafeTemplateRenderer(
            @Value("${notification.delivery.rendered-content-max-bytes:500000}") int maxRenderedBytes,
            @Value("${notification.delivery.subject-max-length:200}") int maxSubjectLength,
            @Value("${notification.delivery.sms-max-length:480}") int maxSmsLength) {
        this.maxRenderedBytes = maxRenderedBytes;
        this.maxSubjectLength = maxSubjectLength;
        this.maxSmsLength = maxSmsLength;
        this.handlebars = new Handlebars();
        registerHelpers();
    }

    public TemplateValidationResult validate(TemplateDocument document, Map<String, Object> data) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        if (document.templateKey() == null || !document.templateKey().matches("[A-Z0-9_]{3,100}")) {
            errors.add("templateKey must contain only uppercase letters, digits, and underscores");
        }
        if (document.locale() == null || !document.locale().matches("[a-z]{2}-[A-Z]{2}")) {
            errors.add("locale must use language-REGION format");
        }
        if (document.subject() != null && document.subject().length() > maxSubjectLength) {
            errors.add("subject exceeds " + maxSubjectLength + " characters");
        }
        if ((document.htmlContent() != null && document.htmlContent().contains("{{{"))
                || (document.textContent() != null && document.textContent().contains("{{{"))
                || (document.subject() != null && document.subject().contains("{{{"))) {
            errors.add("unescaped triple-brace expressions are not allowed");
        }
        validateVariables(document, data, errors);
        validateExpressions(document.subject(), document.variablesSchema(), errors);
        validateExpressions(document.htmlContent(), document.variablesSchema(), errors);
        validateExpressions(document.textContent(), document.variablesSchema(), errors);
        if (document.channel() == com.project.notificationservice.domain.NotificationTypes.Channel.EMAIL
                && (document.htmlContent() == null || document.textContent() == null)) {
            errors.add("email templates require both HTML and plain-text content");
        }
        if (document.channel() == com.project.notificationservice.domain.NotificationTypes.Channel.SMS
                && document.textContent() != null && document.textContent().length() > maxSmsLength) {
            warnings.add("SMS source may exceed the channel character limit after rendering");
        }
        return new TemplateValidationResult(errors.isEmpty(), List.copyOf(errors), List.copyOf(warnings));
    }

    public RenderedTemplate render(TemplateDocument document, Map<String, Object> values) {
        TemplateValidationResult validation = validate(document, values);
        if (!validation.valid()) {
            throw new NotificationException(
                    "TEMPLATE_VALIDATION_FAILED",
                    String.join("; ", validation.errors()),
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }
        try {
            String subject = apply(document.subject(), values);
            String html = apply(document.htmlContent(), values);
            String text = apply(document.textContent(), values);
            html = HTML_POLICY.sanitize(html == null ? "" : html);
            enforceLimits(document, subject, html, text);
            return new RenderedTemplate(subject, html, text);
        } catch (NotificationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new NotificationException(
                    "TEMPLATE_RENDER_FAILED",
                    "Template rendering failed: " + exception.getMessage(),
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    private void validateVariables(
            TemplateDocument document,
            Map<String, Object> values,
            List<String> errors) {
        Map<String, Object> safeValues = values == null ? Map.of() : values;
        for (Map.Entry<String, VariableDefinition> entry : document.variablesSchema().entrySet()) {
            if (entry.getValue().required()
                    && (!safeValues.containsKey(entry.getKey()) || safeValues.get(entry.getKey()) == null)) {
                errors.add("missing required variable: " + entry.getKey());
            }
        }
        for (String variable : safeValues.keySet()) {
            if (!document.variablesSchema().containsKey(variable)) {
                errors.add("unknown variable: " + variable);
            }
        }
    }

    private void validateExpressions(
            String source,
            Map<String, VariableDefinition> schema,
            List<String> errors) {
        if (source == null)
            return;
        Matcher matcher = EXPRESSION.matcher(source);
        while (matcher.find()) {
            String expression = matcher.group(1).trim();
            if (expression.startsWith("!") || expression.startsWith("#") || expression.startsWith("/")
                    || expression.startsWith("else")) {
                errors.add("blocks, partials, comments, and expression logic are not allowed: " + expression);
                continue;
            }
            String[] tokens = expression.split("\\s+");
            String first = cleanToken(tokens[0]);
            if (HELPERS.contains(first)) {
                for (int index = 1; index < tokens.length; index++) {
                    String token = cleanToken(tokens[index]);
                    if (!isLiteral(tokens[index]) && !schema.containsKey(token)) {
                        errors.add("unknown helper argument: " + token);
                    }
                }
            } else if (!schema.containsKey(first)) {
                errors.add("unsupported helper or variable: " + first);
            }
        }
    }

    private boolean isLiteral(String token) {
        String cleaned = cleanToken(token);
        return token.startsWith("\"") || token.startsWith("'") || token.matches("-?\\d+(\\.\\d+)?")
                || LITERALS.contains(cleaned);
    }

    private String cleanToken(String token) {
        return token.replaceAll("^[\"']|[\"']$", "");
    }

    private String apply(String source, Map<String, Object> values) throws Exception {
        if (source == null)
            return "";
        Template template = handlebars.compileInline(source);
        return template.apply(values == null ? Map.of() : values);
    }

    private void enforceLimits(TemplateDocument document, String subject, String html, String text) {
        if (subject.length() > maxSubjectLength) {
            throw new NotificationException("SUBJECT_TOO_LONG", "Rendered subject is too long",
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }
        int size = subject.getBytes(java.nio.charset.StandardCharsets.UTF_8).length
                + html.getBytes(java.nio.charset.StandardCharsets.UTF_8).length
                + text.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
        if (size > maxRenderedBytes) {
            throw new NotificationException("RENDERED_CONTENT_TOO_LARGE", "Rendered content is too large",
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }
        if (document.channel() == com.project.notificationservice.domain.NotificationTypes.Channel.SMS
                && text.length() > maxSmsLength) {
            throw new NotificationException("SMS_TOO_LONG", "Rendered SMS is too long",
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    private void registerHelpers() {
        handlebars.registerHelper("uppercase",
                (value, options) -> value == null ? ""
                        : String.valueOf(value).toUpperCase(options.context.model() instanceof Locale locale
                                ? locale
                                : Locale.ROOT));
        handlebars.registerHelper("lowercase",
                (value, options) -> value == null ? "" : String.valueOf(value).toLowerCase(Locale.ROOT));
        handlebars.registerHelper("join", (value, options) -> {
            if (!(value instanceof Collection<?> collection))
                return "";
            String delimiter = options.param(0, ", ");
            return collection.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(delimiter));
        });
        handlebars.registerHelper("defaultValue",
                (value, options) -> value == null || String.valueOf(value).isBlank() ? options.param(0, "") : value);
        handlebars.registerHelper("maskEmail", (value, options) -> maskEmail(value));
        handlebars.registerHelper("maskPhone", (value, options) -> maskPhone(value));
        handlebars.registerHelper("formatCurrency", (value, options) -> formatCurrency(value, options));
        handlebars.registerHelper("formatDate", (value, options) -> formatTemporal(value, "dd/MM/yyyy"));
        handlebars.registerHelper("formatTime", (value, options) -> formatTemporal(value, "HH:mm"));
        handlebars.registerHelper("formatDateTime", (value, options) -> formatTemporal(value, "dd/MM/yyyy HH:mm"));
    }

    private String maskEmail(Object value) {
        String email = value == null ? "" : String.valueOf(value);
        int marker = email.indexOf('@');
        if (marker < 2)
            return "***";
        return email.substring(0, 2) + "***" + email.substring(marker);
    }

    private String maskPhone(Object value) {
        String phone = value == null ? "" : String.valueOf(value);
        return phone.length() < 4 ? "***" : "***" + phone.substring(phone.length() - 4);
    }

    private String formatCurrency(Object value, Options options) {
        if (value == null)
            return "";
        BigDecimal amount = new BigDecimal(String.valueOf(value));
        String currency = options.param(0, "VND");
        NumberFormat format = NumberFormat.getCurrencyInstance(
                "VND".equalsIgnoreCase(currency) ? Locale.forLanguageTag("vi-VN") : Locale.US);
        return format.format(amount);
    }

    private String formatTemporal(Object value, String pattern) {
        if (value == null)
            return "";
        OffsetDateTime temporal;
        if (value instanceof Instant instant) {
            temporal = instant.atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toOffsetDateTime();
        } else {
            temporal = OffsetDateTime.parse(String.valueOf(value));
        }
        return DateTimeFormatter.ofPattern(pattern).format(temporal);
    }
}
