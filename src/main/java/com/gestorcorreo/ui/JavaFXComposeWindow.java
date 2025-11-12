package com.gestorcorreo.ui;

import com.gestorcorreo.model.EmailConfig;
import com.gestorcorreo.service.EmailSendService;
import com.gestorcorreo.service.AddressHistoryService;
import com.gestorcorreo.model.EmailMessage;
import com.gestorcorreo.model.Attachment;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.web.HTMLEditor;
import javafx.stage.FileChooser;
import org.languagetool.JLanguageTool;
import org.languagetool.language.Spanish;
import org.languagetool.rules.RuleMatch;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.Priority;
import javafx.scene.Node;
import javafx.scene.shape.SVGPath;
import javafx.scene.paint.Color;
import javafx.scene.layout.StackPane;
import javafx.scene.Group;
import java.util.prefs.Preferences;
import java.time.LocalDateTime;
import com.gestorcorreo.service.EmailStorageService;
import javafx.animation.PauseTransition;
import javafx.util.Duration;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Ventana de composición basada en JavaFX (sin Swing), para mejor soporte de teclas muertas/acentos.
 */
public class JavaFXComposeWindow {
    private static final String PREF_KEY_DARK_THEME = "darkTheme";

    public static void open(EmailConfig config) {
        openWithPrefill(config, null, null, null);
    }

    public static void openReply(EmailConfig config, EmailMessage original) {
        String replyTo = extractEmail(original != null ? original.getFrom() : null);
        String subject = original != null && original.getSubject() != null ?
                (original.getSubject().matches("(?i)^Re: .*") ? original.getSubject() : "Re: " + original.getSubject()) : "Re:";
        String quoted = buildQuotedHtml(original, false);
        openWithPrefill(config, replyTo, subject, quoted);
    }

    public static void openForward(EmailConfig config, EmailMessage original) {
        String subject = original != null && original.getSubject() != null ?
                (original.getSubject().matches("(?i)^Fwd: .*") ? original.getSubject() : "Fwd: " + original.getSubject()) : "Fwd:";
        String quoted = buildQuotedHtml(original, true);
        openWithPrefill(config, null, subject, quoted);
    }

    private static void openWithPrefill(EmailConfig config, String toPrefill, String subjectPrefill, String htmlPrefill) {
        Platform.runLater(() -> {
            Stage stage = new Stage();
            stage.setTitle("Nuevo mensaje");
            stage.initModality(Modality.NONE);

            // Campos
            // Chips + campo interno para 'Para'
            FlowPane toChips = new FlowPane();
            toChips.setHgap(4);
            toChips.setVgap(4);
            toChips.setPrefWrapLength(600);
            TextField toField = new TextField();
            toField.setPromptText("Introduce direcciones y pulsa Enter o coma");
            toField.addEventFilter(KeyEvent.KEY_PRESSED, ev -> {
                var code = ev.getCode();
                if (code == javafx.scene.input.KeyCode.ENTER || code == javafx.scene.input.KeyCode.COMMA) {
                    ev.consume();
                    commitAddressChip(toField, toChips);
                } else if (code == javafx.scene.input.KeyCode.BACK_SPACE) {
                    if (toField.getText().isEmpty() && !toChips.getChildren().isEmpty()) {
                        toChips.getChildren().remove(toChips.getChildren().size()-1);
                    }
                }
            });
            // Autocompletado en vivo tras 2+ caracteres
            ContextMenu autoMenu = new ContextMenu();
            toField.textProperty().addListener((obs, oldV, newV) -> {
                if (newV != null && newV.trim().length() >= 2) {
                    var suggestions = AddressHistoryService.getInstance().suggest(newV.trim(), 6);
                    if (!suggestions.isEmpty()) {
                        autoMenu.getItems().clear();
                        for (String s : suggestions) {
                            MenuItem mi = new MenuItem(s);
                            mi.setOnAction(a -> {
                                toField.setText(s);
                                toField.positionCaret(s.length());
                            });
                            autoMenu.getItems().add(mi);
                        }
                        if (!autoMenu.isShowing()) autoMenu.show(toField, javafx.geometry.Side.BOTTOM, 0, 0);
                    } else {
                        autoMenu.hide();
                    }
                } else {
                    autoMenu.hide();
                }
            });
            // Indicador visual de validez email en on-the-fly
            toField.textProperty().addListener((o,ov,nv)-> {
                if (nv == null || nv.isBlank()) {
                    toField.setStyle("");
                } else if (isValidEmailFragment(nv)) {
                    toField.setStyle("-fx-background-color: #2d4830; -fx-text-fill: #e6e6e6;");
                } else {
                    toField.setStyle("-fx-background-color: #5a2e2e; -fx-text-fill: #e6e6e6;");
                }
            });
            Button clearToBtn = new Button("Limpiar");
            clearToBtn.setOnAction(e -> { toChips.getChildren().clear(); toField.clear(); });
            TextField ccField = new TextField();
            TextField bccField = new TextField();
            TextField subjectField = new TextField();
            // Editor enriquecido básico con HTML (para formato)
            HTMLEditor htmlEditor = new HTMLEditor();
            htmlEditor.setPrefHeight(400);
            htmlEditor.setMinHeight(300);
            htmlEditor.setMaxHeight(Double.MAX_VALUE);
            htmlEditor.setStyle("-fx-font-family: 'DejaVu Sans','Noto Sans','Liberation Sans','SansSerif';");

            // Prefills (si se proporcionan)
            if (toPrefill != null && !toPrefill.isBlank()) {
                toField.setText(toPrefill);
                commitAddressChip(toField, toChips);
            }
            if (subjectPrefill != null) subjectField.setText(subjectPrefill);
            if (htmlPrefill != null) htmlEditor.setHtmlText(htmlPrefill);

            // Layout de formulario
            GridPane form = new GridPane();
            form.setHgap(8);
            form.setVgap(8);
            // Columnas: 0 fija (labels), 1 expansible (contenido)
            ColumnConstraints c0 = new ColumnConstraints();
            c0.setHgrow(Priority.NEVER);
            c0.setMinWidth(90);
            ColumnConstraints c1 = new ColumnConstraints();
            c1.setHgrow(Priority.ALWAYS);
            c1.setFillWidth(true);
            form.getColumnConstraints().addAll(c0, c1);
            form.add(new Label("Para"), 0, 0); form.add(wrapChipsArea(toChips, toField, clearToBtn), 1, 0);
            form.add(new Label("CC"), 0, 1); form.add(ccField, 1, 1);
            form.add(new Label("Asunto"), 0, 2); form.add(subjectField, 1, 2);
            form.add(new Label("BCC"), 0, 3); form.add(bccField, 1, 3);
            form.add(new Label("Mensaje"), 0, 4);
            form.add(htmlEditor, 1, 4);
            GridPane.setVgrow(htmlEditor, Priority.ALWAYS);
            GridPane.setHgrow(htmlEditor, Priority.ALWAYS);
            GridPane.setMargin(htmlEditor, new Insets(0, 0, 0, 0));
            // Filas: hacer que el editor crezca, adjuntos a veces
            RowConstraints r0 = new RowConstraints();
            RowConstraints r1 = new RowConstraints();
            RowConstraints r2 = new RowConstraints();
            RowConstraints r3 = new RowConstraints();
            RowConstraints r4 = new RowConstraints(); r4.setVgrow(Priority.ALWAYS);
            RowConstraints r5 = new RowConstraints(); r5.setVgrow(Priority.SOMETIMES);
            RowConstraints r6 = new RowConstraints();
            form.getRowConstraints().addAll(r0, r1, r2, r3, r4, r5, r6);

            // Preferencias de usuario (persistencia de tema)
            Preferences prefs = Preferences.userNodeForPackage(JavaFXComposeWindow.class);
            boolean savedDark = prefs.getBoolean(PREF_KEY_DARK_THEME, true);

            // Botones
            Button sendBtn = new Button("Enviar");
            Button saveDraftBtn = new Button("Guardar borrador");
            Button closeBtn = new Button("Cerrar");
            Button attachBtn = new Button("Adjuntar archivo");
            Button spellCheckBtn = new Button("Revisar ortografía");
            ToggleButton themeToggle = new ToggleButton(savedDark ? "Tema oscuro" : "Tema claro");
            themeToggle.setSelected(savedDark);
            ProgressBar progressBar = new ProgressBar(0);
            progressBar.setPrefWidth(150);
            progressBar.setVisible(false);
            Label status = new Label();
            HBox buttons = new HBox(8, sendBtn, saveDraftBtn, closeBtn, attachBtn, spellCheckBtn, themeToggle, progressBar, status);
            buttons.setAlignment(Pos.CENTER_LEFT);

            // Lista de adjuntos visibles
            ObservableList<Attachment> attachments = FXCollections.observableArrayList();
            ListView<Attachment> attachmentsView = new ListView<>(attachments);
            attachmentsView.setCellFactory(list -> new ListCell<>() {
                @Override
                protected void updateItem(Attachment item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setGraphic(null);
                    } else {
                        setText(item.getFileName() + " (" + (item.getFileSize()/1024) + " KB)");
                    }
                }
            });
            attachmentsView.setPrefHeight(100);
            attachmentsView.setMaxHeight(Double.MAX_VALUE);
            GridPane.setVgrow(attachmentsView, Priority.SOMETIMES);
            form.add(new Label("Adjuntos"), 0, 5);
            form.add(attachmentsView, 1, 5);

            // Enviar como texto plano
            CheckBox plainCheck = new CheckBox("Enviar como texto plano");
            plainCheck.setTooltip(new Tooltip("Si marcas esta opción, el correo se enviará en texto plano (sin formato)."));
            form.add(plainCheck, 1, 6);

            BorderPane root = new BorderPane();
            root.setPadding(new Insets(12));
            root.setCenter(form);
            root.setBottom(buttons);
            BorderPane.setMargin(buttons, new Insets(12, 0, 0, 0));
            form.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

            // Enviar
            sendBtn.setOnAction(ev -> {
                // Limpiar resaltados de ortografía antes de leer el HTML
                clearSpellHighlights(htmlEditor);
                // Convertir imágenes de previsualización (file:) a cid: antes de capturar el HTML
                convertPreviewImagesToCid(htmlEditor);
                String to = toField.getText();
                String cc = ccField.getText();
                String subject = subjectField.getText();
                String html = htmlEditor.getHtmlText();
                String bcc = bccField.getText();

                if (to == null || to.trim().isEmpty()) {
                    status.setText("Falta destinatario");
                    return;
                }
                status.setText("Enviando...");
                sendBtn.setDisable(true);
                progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
                progressBar.setVisible(true);

                // Enviar en background
                new Thread(() -> {
                    try {
                        String[] toArr = to.split("[;,]");
                        String[] ccArr = (cc == null || cc.isBlank()) ? new String[0] : cc.split("[;,]");
                        EmailMessage msg = new EmailMessage();
                        for (String r : toArr) { if (!r.isBlank()) msg.addTo(r.trim()); }
                        for (String r : ccArr) { if (!r.isBlank()) msg.addCc(r.trim()); }
                        if (bcc != null && !bcc.isBlank()) {
                            for (String r : bcc.split("[;,]")) { if (!r.isBlank()) msg.addBcc(r.trim()); }
                        }
                        msg.setSubject(subject == null ? "" : subject);
                        if (plainCheck.isSelected()) {
                            String textPlain = html == null ? "" : html.replaceAll("<[^>]+>", " ").replaceAll("\n\n+","\n").trim();
                            msg.setBody(textPlain);
                            msg.setHtml(false);
                        } else {
                            msg.setBody(html == null ? "" : html);
                            msg.setHtml(true);
                        }
                        msg.setAttachments(new java.util.ArrayList<>(attachments));
                        // Registrar direcciones usadas para autocompletado futuro
                        AddressHistoryService.getInstance().register(to);
                        AddressHistoryService.getInstance().register(cc);
                        AddressHistoryService.getInstance().register(bcc);
                        // También registrar contactos en ContactService para crecimiento automático libreta
                        com.gestorcorreo.service.ContactService cs = com.gestorcorreo.service.ContactService.getInstance();
                        registerIntoContactService(cs, to);
                        registerIntoContactService(cs, cc);
                        registerIntoContactService(cs, bcc);
                        EmailSendService.sendEmail(config, msg);
                        Platform.runLater(() -> {
                            status.setText("Enviado");
                            sendBtn.setDisable(false);
                            progressBar.setVisible(false);
                        });
                    } catch (Exception ex) {
                        Platform.runLater(() -> {
                            status.setText("Error: " + ex.getMessage());
                            sendBtn.setDisable(false);
                            progressBar.setVisible(false);
                        });
                    }
                }, "send-mail").start();
            });

            // Guardar borrador manualmente
            saveDraftBtn.setOnAction(ev -> {
                // Limpiar resaltados de ortografía antes de leer el HTML
                clearSpellHighlights(htmlEditor);
                // Convertir imágenes de previsualización (file:) a cid: antes de capturar el HTML
                convertPreviewImagesToCid(htmlEditor);
                // Construir EmailMessage parcial
                String to = toField.getText();
                String cc = ccField.getText();
                String bcc = bccField.getText();
                String subject = subjectField.getText();
                String html = htmlEditor.getHtmlText();
                EmailMessage draft = new EmailMessage();
                if (to != null) {
                    for (String r : to.split("[;,]")) {
                        if (!r.isBlank()) draft.addTo(r.trim());
                    }
                }
                if (cc != null && !cc.isBlank()) {
                    for (String r : cc.split("[;,]")) {
                        if (!r.isBlank()) draft.addCc(r.trim());
                    }
                }
                if (bcc != null && !bcc.isBlank()) {
                    for (String r : bcc.split("[;,]")) {
                        if (!r.isBlank()) draft.addBcc(r.trim());
                    }
                }
                draft.setSubject(subject == null ? "" : subject);
                // Flag plain eliminado (no se usa directamente, mantenemos body/html)
                if (plainCheck.isSelected()) {
                    String textPlain = html == null ? "" : html.replaceAll("<[^>]+>", " ").replaceAll("\n\n+","\n").trim();
                    draft.setBody(textPlain);
                    draft.setHtml(false);
                } else {
                    draft.setBody(html == null ? "" : html);
                    draft.setHtml(true);
                }
                draft.setSentDate(LocalDateTime.now());
                draft.setAttachments(new java.util.ArrayList<>(attachments));
                // Almacenar como borrador usando carpeta 'Drafts' local
                try {
                    // Cargar borradores existentes
                    java.util.List<EmailMessage> existing = EmailStorageService.getInstance().loadEmails(config.getEmail(), "Drafts");
                    existing.add(0, draft); // añadir al principio
                    EmailStorageService.getInstance().saveEmails(config.getEmail(), "Drafts", existing);
                    status.setText("Borrador guardado");
                } catch (Exception ex) {
                    status.setText("Error guardando borrador: " + ex.getMessage());
                }
            });

            closeBtn.setOnAction(ev -> stage.close());

            // Adjuntar archivo
            attachBtn.setOnAction(ev -> {
                FileChooser fc = new FileChooser();
                fc.setTitle("Seleccionar archivo adjunto");
                java.io.File f = fc.showOpenDialog(stage);
                if (f != null) {
                    Attachment att = new Attachment(f);
                    attachments.add(att);
                }
            });

            // Añadir botón de "Insertar imagen" dentro de la toolbar del HTMLEditor
            addImageButtonToEditorToolbar(htmlEditor, stage, attachments);

            // Corrector ortográfico con LanguageTool (español)
            spellCheckBtn.setOnAction(ev -> {
                try {
                    JLanguageTool tool = new JLanguageTool(new Spanish());
                    String textPlain = htmlEditor.getHtmlText().replaceAll("<[^>]+>", " ");
                    var matches = tool.check(textPlain);
                    if (matches.isEmpty()) {
                        status.setText("Ortografía OK");
                    } else {
                        // Mostrar resumen simple por ahora
                        StringBuilder sb = new StringBuilder();
                        int max = Math.min(5, matches.size());
                        for (int i = 0; i < max; i++) {
                            RuleMatch m = matches.get(i);
                            sb.append("• ").append(m.getMessage());
                            var sugg = m.getSuggestedReplacements();
                            if (!sugg.isEmpty()) sb.append(" → ").append(String.join(", ", sugg.subList(0, Math.min(3, sugg.size()))));
                            sb.append("\n");
                        }
                        if (matches.size() > max) sb.append("(y ").append(matches.size()-max).append(" más)");
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Revisión ortográfica");
                        alert.setHeaderText("Posibles mejoras");
                        alert.setContentText(sb.toString());
                        alert.showAndWait();
                    }
                } catch (Exception ex) {
                    status.setText("Error corrector: " + ex.getMessage());
                }
            });

            Scene scene = new Scene(root, 900, 700);
            // Establecer fondo inicial (se ajustará en applyTheme)
            root.setStyle(savedDark ? "-fx-background-color: #2b2b2b;" : "-fx-background-color: #fafafa;");
            stage.setScene(scene);
            // Aplicar tema guardado al contenido inicial
            applyTheme(scene, htmlEditor, savedDark);

            // Toggle para alternar tema
            themeToggle.setOnAction(e -> {
                boolean dark = themeToggle.isSelected();
                themeToggle.setText(dark ? "Tema oscuro" : "Tema claro");
                applyTheme(scene, htmlEditor, dark);
                // Persistir preferencia
                prefs.putBoolean(PREF_KEY_DARK_THEME, dark);
            });

            // Revisión ortográfica en vivo (debounced)
            setupLiveSpellcheck(htmlEditor);
            stage.show();
        });
    }

    // Envolver un TextField con botones de ayuda (libreta y sugerencias)
    

    private static boolean isValidEmailFragment(String text) {
        return text.matches("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]*");
    }

    private static boolean isValidEmail(String email) {
        return email.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    private static void commitAddressChip(TextField field, FlowPane container) {
        String raw = field.getText();
        if (raw == null || raw.trim().isEmpty()) return;
    String email = raw.trim().replaceAll("[;,]+$", "");
        if (!isValidEmail(email)) { field.clear(); return; }
        Label chip = new Label(email);
        Button close = new Button("x");
        close.setOnAction(e -> container.getChildren().remove(chip.getParent()));
        HBox wrapper = new HBox(4, chip, close);
        wrapper.setStyle("-fx-padding:2 6; -fx-background-color:#4e5254; -fx-background-radius:12; -fx-alignment: center;");
        container.getChildren().add(wrapper);
        field.clear();
    }

    private static HBox wrapChipsArea(FlowPane chips, TextField field, Button clearBtn) {
        VBox vbox = new VBox(4, chips, field);
        Button bookBtn = new Button("Libreta...");
        bookBtn.setOnAction(e -> openAddressBookAndFill(field));
        MenuButton suggestBtn = new MenuButton("Sugerir");
        suggestBtn.setOnShowing(ev -> {
            suggestBtn.getItems().clear();
            var suggestions = AddressHistoryService.getInstance().suggest(field.getText(), 8);
            for (String s : suggestions) {
                MenuItem mi = new MenuItem(s);
                mi.setOnAction(a -> insertAddress(field, s));
                suggestBtn.getItems().add(mi);
            }
            if (suggestBtn.getItems().isEmpty()) {
                MenuItem none = new MenuItem("(sin sugerencias)");
                none.setDisable(true);
                suggestBtn.getItems().add(none);
            }
        });
        HBox controls = new HBox(6, clearBtn, bookBtn, suggestBtn);
        HBox box = new HBox(6, vbox, controls);
        HBox.setHgrow(vbox, javafx.scene.layout.Priority.ALWAYS);
        return box;
    }

    private static void insertAddress(TextField field, String email) {
        String current = field.getText();
        if (current == null || current.isBlank()) {
            field.setText(email);
        } else if (current.trim().endsWith(";") || current.trim().endsWith(",")) {
            field.setText(current + " " + email);
        } else {
            field.setText(current + ", " + email);
        }
    }

    private static void openAddressBookAndFill(TextField targetField) {
        // Reutilizar el servicio para obtener contactos y mostrar un diálogo JavaFX ligero de selección múltiple
        var contacts = com.gestorcorreo.service.ContactService.getInstance().getAllContacts();
        javafx.collections.ObservableList<String> items = javafx.collections.FXCollections.observableArrayList();
        for (var c : contacts) {
            items.add((c.getName() != null && !c.getName().isBlank() ? c.getName() + " <" + c.getEmail() + ">" : c.getEmail()));
        }

        ListView<String> list = new ListView<>(items);
        list.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        list.setPrefHeight(300);

        Dialog<java.util.List<String>> dialog = new Dialog<>();
        dialog.setTitle("Seleccionar destinatarios");
        dialog.getDialogPane().setContent(list);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.setResultConverter(bt -> bt == ButtonType.OK ? list.getSelectionModel().getSelectedItems() : null);
        var result = dialog.showAndWait();
        result.ifPresent(selected -> {
            for (String s : selected) {
                String email = extractEmail(s);
                if (email != null) insertAddress(targetField, email);
            }
        });
    }

    private static String extractEmail(String s) {
        if (s == null) return null;
        int lt = s.indexOf('<');
        int gt = s.indexOf('>');
        if (lt >= 0 && gt > lt) {
            return s.substring(lt+1, gt).trim();
        }
        return s.trim();
    }

    private static String htmlEscape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String buildQuotedHtml(EmailMessage original, boolean forward) {
        if (original == null) return "";
        String header = forward ? "--- Mensaje reenviado ---" : "--- Mensaje original ---";
        String from = original.getFrom() != null ? original.getFrom() : "";
        String subj = original.getSubject() != null ? original.getSubject() : "";
        String date = original.getSentDate() != null ? original.getSentDate().toString() : "";
        String body = original.getBody() != null ? original.getBody() : "";
        // Si el original era HTML, escapa para no romper nuestro editor al citarlo simple
        String bodyEsc = htmlEscape(body);
        String quoted = "<div><br><br>" +
                "<p>" + header + "</p>" +
                "<p>De: " + htmlEscape(from) + "<br>Fecha: " + htmlEscape(date) + "<br>Asunto: " + htmlEscape(subj) + "</p>" +
                "<blockquote style=\"border-left:3px solid #888;padding-left:8px; color:#666;\">" + bodyEsc.replace("\n", "<br>") + "</blockquote>" +
                "</div>";
        return quoted;
    }

    private static void registerIntoContactService(com.gestorcorreo.service.ContactService cs, String rawList) {
        if (rawList == null) return;
        for (String part : rawList.split("[;,]")) {
            String email = part.trim();
            if (!email.isEmpty() && email.contains("@")) {
                cs.addOrUpdateFromEmail(email, null);
            }
        }
    }

    private static void applyTheme(Scene scene, HTMLEditor editor, boolean dark) {
        // Limpiar estilos previos (dark/light) y aplicar el nuevo
        var darkUrl = JavaFXComposeWindow.class.getResource("/styles/compose-dark.css");
        var lightUrl = JavaFXComposeWindow.class.getResource("/styles/compose-light.css");
        if (darkUrl != null) scene.getStylesheets().remove(darkUrl.toExternalForm());
        if (lightUrl != null) scene.getStylesheets().remove(lightUrl.toExternalForm());
        if (dark) {
            if (darkUrl != null) scene.getStylesheets().add(darkUrl.toExternalForm());
        } else {
            if (lightUrl != null) scene.getStylesheets().add(lightUrl.toExternalForm());
        }
        // WebView interno del HTMLEditor
        Platform.runLater(() -> {
            try {
                var node = editor.lookup("WebView");
                if (node instanceof javafx.scene.web.WebView webView) {
                    var css = JavaFXComposeWindow.class.getResource(dark ? "/styles/htmleditor-dark.css" : "/styles/htmleditor-light.css");
                    if (css != null) webView.getEngine().setUserStyleSheetLocation(css.toExternalForm());
                    webView.setStyle(dark ? "-fx-background-color: #3c3f41;" : "-fx-background-color: #ffffff;");
                }
            } catch (Exception ignored) {}
        });
        // Fondo raíz
        if (scene.getRoot() instanceof BorderPane bp) {
            bp.setStyle(dark ? "-fx-background-color:#2b2b2b;" : "-fx-background-color:#fafafa;");
        }
    }

    private static void addImageButtonToEditorToolbar(HTMLEditor htmlEditor, Stage stage, ObservableList<Attachment> attachments) {
        // Inyectar el botón una vez que la Skin/toolbar estén creadas
        Platform.runLater(() -> {
            try {
                var toolbars = htmlEditor.lookupAll(".tool-bar");
                javafx.scene.control.ToolBar target = null;
                for (Node n : toolbars) {
                    if (n instanceof javafx.scene.control.ToolBar tb) {
                        target = tb; // nos quedamos con la última barra encontrada
                    }
                }
                if (target == null) return;
                Button imgBtn = new Button();
                imgBtn.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                // Crear icono vectorial simple (marco + montaña + sol)
                SVGPath frame = new SVGPath();
                frame.setContent("M2 2 H14 V14 H2 Z M3 3 V13 H13 V3 Z"); // marco doble
                frame.setFill(Color.TRANSPARENT);
                frame.setStroke(Color.web("#888"));
                frame.setStrokeWidth(1.1);

                SVGPath mountain = new SVGPath();
                mountain.setContent("M4 11 L7 7 L9.5 9.8 L11.5 6 L14 11 Z");
                mountain.setFill(Color.web("#5a8"));
                mountain.setStroke(Color.web("#4a7"));
                mountain.setStrokeWidth(0.8);

                SVGPath sun = new SVGPath();
                sun.setContent("M9.8 5.2 A1.6 1.6 0 1 1 6.6 5.2 A1.6 1.6 0 1 1 9.8 5.2 Z");
                sun.setFill(Color.web("#e0c44b"));
                sun.setStroke(Color.web("#c5a732"));
                sun.setStrokeWidth(0.6);

                Group iconGroup = new Group(frame, mountain, sun);
                StackPane iconPane = new StackPane(iconGroup);
                iconPane.setPrefSize(18,18);
                iconPane.setMinSize(18,18);
                iconPane.setMaxSize(18,18);
                // Suavizar en tema oscuro/claro adaptando trazo
                iconPane.styleProperty().bind(htmlEditor.sceneProperty().map(s -> {
                    boolean dark = s != null && s.getStylesheets().stream().anyMatch(st -> st.contains("compose-dark.css"));
                    return dark ? "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 2,0,0,0);" : ""; }));
                imgBtn.setGraphic(iconPane);
                imgBtn.getStyleClass().add("htmleditor-image-btn");
                imgBtn.setPrefSize(26,26);
                imgBtn.setMinSize(26,26);
                imgBtn.setMaxSize(26,26);
                imgBtn.setFocusTraversable(false);
                imgBtn.setTooltip(new Tooltip("Insertar imagen en el cuerpo"));
                imgBtn.setOnAction(ev -> {
                    FileChooser fc = new FileChooser();
                    fc.setTitle("Insertar imagen");
                    fc.getExtensionFilters().addAll(
                        new FileChooser.ExtensionFilter("Imágenes", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp")
                    );
                    java.io.File f = fc.showOpenDialog(stage);
                    if (f != null) {
                        Attachment att = new Attachment(f);
                        att.setInline(true);
                        String cid = java.util.UUID.randomUUID().toString() + "@cid";
                        att.setContentId(cid);
                        attachments.add(att);

                        String current = htmlEditor.getHtmlText();
                        String fileUri = f.toURI().toString();
                        // Para previsualización usamos file: y guardamos data-original-cid para convertir antes de enviar
                        String imgTag = "<p><img class=\"mail-img\" src=\"" + fileUri + "\" data-original-cid=\"cid:" + cid + "\" alt=\"imagen\" style=\"max-width:600px;height:auto;\"></p>";
                        if (current == null || current.isBlank() || !current.toLowerCase().contains("<body")) {
                            current = "<html><head><meta charset='UTF-8'></head><body>" + imgTag + "</body></html>";
                        } else {
                            current = current.replaceFirst("(?i)(</body>)", imgTag + "$1");
                        }
                        htmlEditor.setHtmlText(current);
                        // Inyectar script de redimensionamiento (idempotente) y seleccionar la última imagen añadida
                        Platform.runLater(() -> {
                            try {
                                var node = htmlEditor.lookup("WebView");
                                if (node instanceof javafx.scene.web.WebView webView) {
                                    String script = "(function(){" +
                                            "if(window.__imgResizeInit) return;" +
                                            "window.__imgResizeInit=true;" +
                                            "function clearWrappers(){document.querySelectorAll('span.__img-wrapper').forEach(function(w){var img=w.querySelector('img'); if(img){w.parentNode.insertBefore(img,w); } w.remove();});}" +
                                            "function wrap(img){ if(img.closest('span.__img-wrapper')) return; var span=document.createElement('span'); span.className='__img-wrapper'; span.style.position='relative'; img.parentNode.insertBefore(span,img); span.appendChild(img); var handle=document.createElement('div'); handle.className='__img-handle'; handle.style.cssText='position:absolute;width:12px;height:12px;right:-6px;bottom:-6px;background:#4a90e2;border-radius:50%;cursor:nwse-resize;box-shadow:0 0 2px rgba(0,0,0,.5);'; span.appendChild(handle); handle.addEventListener('mousedown', function(ev){ ev.preventDefault(); ev.stopPropagation(); var startX=ev.clientX; var startW=img.getBoundingClientRect().width; function mm(mv){ var dx=mv.clientX-startX; var nw=Math.max(50, Math.min(800, startW+dx)); img.style.width=nw+'px'; img.style.height='auto'; } function mu(){ document.removeEventListener('mousemove', mm); document.removeEventListener('mouseup', mu);} document.addEventListener('mousemove', mm); document.addEventListener('mouseup', mu);});}" +
                                            "document.addEventListener('click', function(e){ if(e.target && e.target.tagName==='IMG'){ document.querySelectorAll('img.__selected-img').forEach(function(im){im.classList.remove('__selected-img'); im.style.outline=''; }); e.target.classList.add('__selected-img'); e.target.style.outline='2px solid #4a90e2'; wrap(e.target); } else { document.querySelectorAll('img.__selected-img').forEach(function(im){im.classList.remove('__selected-img'); im.style.outline=''; }); }});" +
                                            "})();";
                                    webView.getEngine().executeScript(script);
                                }
                            } catch (Exception ignored) { }
                        });
                    }
                });
                // Separador visual y el botón
                target.getItems().add(new Separator());
                target.getItems().add(imgBtn);
            } catch (Exception ignored) {}
        });
    }

    // --- Ortografía en vivo ---
    private static void setupLiveSpellcheck(HTMLEditor editor) {
        PauseTransition debounce = new PauseTransition(Duration.millis(600));
        editor.addEventFilter(KeyEvent.KEY_TYPED, e -> {
            debounce.stop();
            debounce.setOnFinished(ev -> runSpellcheck(editor));
            debounce.playFromStart();
        });
        // También al pegar (CTRL+V) y borrar (KEY_RELEASED)
        editor.addEventFilter(KeyEvent.KEY_RELEASED, e -> {
            switch (e.getCode()) {
                case BACK_SPACE: case DELETE: case ENTER: case SPACE:
                    debounce.stop();
                    debounce.setOnFinished(ev -> runSpellcheck(editor));
                    debounce.playFromStart();
                    break;
                default: break;
            }
        });
    }

    private static void runSpellcheck(HTMLEditor editor) {
        Platform.runLater(() -> {
            try {
                var node = editor.lookup("WebView");
                if (!(node instanceof javafx.scene.web.WebView webView)) return;
                Object innerText = webView.getEngine().executeScript("document && document.body ? document.body.innerText : ''");
                String text = innerText != null ? innerText.toString() : "";
                if (text.isBlank()) {
                    clearSpellHighlights(editor);
                    return;
                }
                // Ejecutar LanguageTool
                JLanguageTool tool = new JLanguageTool(new Spanish());
                var matches = tool.check(text);
                if (matches.isEmpty()) {
                    clearSpellHighlights(editor);
                    return;
                }
                // Palabras erróneas (por posición) -> deduplicadas
                Set<String> wrongWords = new LinkedHashSet<>();
                for (RuleMatch m : matches) {
                    int from = Math.max(0, m.getFromPos());
                    int to = Math.min(text.length(), m.getToPos());
                    if (to > from && (to - from) <= 40) {
                        String w = text.substring(from, to).trim();
                        // Filtrar tokens razonables
                        if (w.matches("[A-Za-zÀ-ÿ]{2,}")) wrongWords.add(w);
                    }
                    if (wrongWords.size() >= 150) break; // límite de seguridad
                }
                // Aplicar resaltado vía JS
                highlightWordsInWebView(webView, wrongWords);
            } catch (Exception ignored) { }
        });
    }

    private static void clearSpellHighlights(HTMLEditor editor) {
        Platform.runLater(() -> {
            try {
                var node = editor.lookup("WebView");
                if (!(node instanceof javafx.scene.web.WebView webView)) return;
                String js = "(function(){\n" +
                        "  if(!document || !document.body) return;\n" +
                        "  var spans = document.querySelectorAll('span.spell-error');\n" +
                        "  spans.forEach(function(s){ var p=s.parentNode; while(s.firstChild){ p.insertBefore(s.firstChild, s); } p.removeChild(s); });\n" +
                        "})();";
                webView.getEngine().executeScript(js);
            } catch (Exception ignored) {}
        });
    }

    // Convierte las imágenes de previsualización (file:) a cid: usando atributo data-original-cid
    private static void convertPreviewImagesToCid(HTMLEditor editor) {
        try {
            var node = editor.lookup("WebView");
            if (!(node instanceof javafx.scene.web.WebView webView)) return;
            String js = "(function(){" +
                    "// Deshacer wrappers de redimensionamiento" +
                    "document.querySelectorAll('span.__img-wrapper').forEach(function(w){var img=w.querySelector('img'); if(img){ w.parentNode.insertBefore(img,w);} w.remove();});" +
                    "var imgs=document.querySelectorAll('img[data-original-cid]');" +
                    "imgs.forEach(function(img){var cid=img.getAttribute('data-original-cid'); if(cid){img.setAttribute('src', cid); img.removeAttribute('data-original-cid'); img.style.outline='';}});" +
                    "})();";
            webView.getEngine().executeScript(js);
        } catch (Exception ignored) {}
    }

    private static void highlightWordsInWebView(javafx.scene.web.WebView webView, Set<String> words) {
        // Preparar JSON con palabras
        String json = new com.google.gson.Gson().toJson(words.stream().filter(w -> w.length() > 1).collect(Collectors.toList()));
        String js = "(function(words){\n" +
                "  if(!document || !document.body) return;\n" +
                // Limpiar primero
                "  var spans = document.querySelectorAll('span.spell-error');\n" +
                "  spans.forEach(function(s){ var p=s.parentNode; while(s.firstChild){ p.insertBefore(s.firstChild, s); } p.removeChild(s); });\n" +
                // TreeWalker sobre nodos de texto
                "  function wrapRangeInTextNode(node, start, end){\n" +
                "    var text = node.nodeValue;\n" +
                "    var before = document.createTextNode(text.slice(0, start));\n" +
                "    var middle = document.createElement('span'); middle.className='spell-error'; middle.textContent = text.slice(start, end);\n" +
                "    var after = document.createTextNode(text.slice(end));\n" +
                "    var parent = node.parentNode;\n" +
                "    parent.insertBefore(before, node);\n" +
                "    parent.insertBefore(middle, node);\n" +
                "    parent.insertBefore(after, node);\n" +
                "    parent.removeChild(node);\n" +
                "    return after;\n" +
                "  }\n" +
                "  function highlightWord(word){\n" +
                "    if(!word || word.length<2) return;\n" +
                "    var wl = word.toLowerCase();\n" +
                "    var walker = document.createTreeWalker(document.body, NodeFilter.SHOW_TEXT, {\n" +
                "      acceptNode: function(n){ if(!n.nodeValue || !n.nodeValue.trim()) return NodeFilter.FILTER_REJECT; if(n.parentNode && n.parentNode.closest && n.parentNode.closest('span.spell-error')) return NodeFilter.FILTER_REJECT; return NodeFilter.FILTER_ACCEPT; }\n" +
                "    });\n" +
                "    var node;\n" +
                "    while((node = walker.nextNode())){\n" +
                "      var txt = node.nodeValue; var lower = txt.toLowerCase(); var idx=0;\n" +
                "      while((idx = lower.indexOf(wl, idx)) !== -1){\n" +
                "        // Limitar a coincidencias por palabra (bordes alfanuméricos)\n" +
                "        var left = idx-1 < 0 ? ' ' : lower.charAt(idx-1);\n" +
                "        var right = idx+wl.length >= lower.length ? ' ' : lower.charAt(idx+wl.length);\n" +
                "        var isBoundary = !(/[a-záéíóúüñ]/i.test(left)) && !(/[a-záéíóúüñ]/i.test(right));\n" +
                "        if(isBoundary){\n" +
                "          node = wrapRangeInTextNode(node, idx, idx+wl.length);\n" +
                "          lower = node.nodeValue.toLowerCase();\n" +
                "          idx += 0; // continuar desde el comienzo del nuevo nodo\n" +
                "        } else { idx += wl.length; }\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "  (words||[]).forEach(highlightWord);\n" +
                "})" + "(" + json + ");";
        try { webView.getEngine().executeScript(js); } catch (Exception ignored) {}
    }
}
