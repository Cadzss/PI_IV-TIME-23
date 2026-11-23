// Autor: Cadu Spadari
// Alterações: Lucas Canto — método destino() usado para gravar o aviso como "pendente" antes do envio
package br.puc.evasao.servidor;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.bson.Document;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Date;
import java.util.Properties;

/**
 * E-mail de alerta de risco ALTO. Sem SMTP, grava em logs/emails.log.
 * Variáveis: EMAIL_COORDENADOR, EMAIL_REMETENTE, SMTP_HOST, SMTP_PORT, SMTP_USUARIO, SMTP_SENHA.
 */
public class NotificadorEmail {

    /** Quem recebe os alertas. */
    public static String destino() {
        return getenv("EMAIL_COORDENADOR", "coordenador@escola.local");
    }

    public Document notificar(String assunto, String corpo) {
        String destino = destino();
        String host = getenv("SMTP_HOST", "");
        Document email = new Document("destino", destino)
                .append("assunto", assunto)
                .append("corpo", corpo)
                .append("criadoEm", new Date());

        // Sem servidor SMTP configurado (caso da demonstração): não envia, só grava uma cópia no log
        if (host.isEmpty()) {
            gravarCopia(destino, assunto, corpo, "SMTP não configurado");
            return email.append("status", "copia_local")
                    .append("motivo", "Defina SMTP_HOST para enviar de verdade. A cópia ficou em logs/emails.log.");
        }
        try {
            enviar(host, destino, assunto, corpo);
            gravarCopia(destino, assunto, corpo, "enviado");
            return email.append("status", "enviado");
        } catch (Exception e) {
            gravarCopia(destino, assunto, corpo, "falha: " + e.getMessage());
            return email.append("status", "falha").append("motivo", e.getMessage());
        }
    }

    private void enviar(String host, String destino, String assunto, String corpo) throws Exception {
        String porta = getenv("SMTP_PORT", "587");
        String usuario = getenv("SMTP_USUARIO", "");
        String senha = getenv("SMTP_SENHA", "");
        String remetente = getenv("EMAIL_REMETENTE", usuario.isEmpty() ? "early-warning@localhost" : usuario);

        Properties props = new Properties();
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", porta);
        props.put("mail.smtp.auth", usuario.isEmpty() ? "false" : "true");
        props.put("mail.smtp.starttls.enable", "true"); // conexão criptografada com o servidor de e-mail
        props.put("mail.smtp.connectiontimeout", "8000");
        props.put("mail.smtp.timeout", "8000");

        Session session = usuario.isEmpty()
                ? Session.getInstance(props)
                : Session.getInstance(props, new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(usuario, senha);
                    }
                });

        MimeMessage mensagem = new MimeMessage(session);
        mensagem.setFrom(new InternetAddress(remetente));
        mensagem.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destino));
        mensagem.setSubject(assunto, "UTF-8");
        mensagem.setText(corpo, "UTF-8");
        Transport.send(mensagem);
    }

    // synchronized: a fila de e-mails e outras threads podem escrever no mesmo arquivo ao mesmo tempo
    private static synchronized void gravarCopia(String destino, String assunto, String corpo, String status) {
        try {
            Path arquivo = Paths.get("logs", "emails.log");
            Files.createDirectories(arquivo.getParent());
            String bloco = "----\nstatus: " + status
                    + "\npara: " + destino
                    + "\nassunto: " + assunto
                    + "\n" + corpo + "\n";
            Files.write(arquivo, bloco.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            System.out.println("[E-mail] " + status + " -> " + destino + " | " + assunto);
        } catch (Exception e) {
            System.out.println("[E-mail] Não foi possível gravar a cópia: " + e.getMessage());
        }
    }

    private static String getenv(String nome, String padrao) {
        String valor = System.getenv(nome);
        return valor == null || valor.isEmpty() ? padrao : valor;
    }
}