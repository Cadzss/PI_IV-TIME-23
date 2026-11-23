// Autor: Lucas Canto
package br.puc.evasao.servidor;

import org.bson.Document;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sessões abertas no login. O token vai para o navegador e volta em todo pedido;
 * é assim que o servidor sabe quem está pedindo (e não confia no que o navegador diz).
 * Fica só em memória: reiniciar o servidor desloga todo mundo.
 */
public class Sessoes {

    static final long DURACAO_MS = 8L * 60 * 60 * 1000; // 8 horas

    private static final SecureRandom ALEATORIO = new SecureRandom();

    // ConcurrentHashMap porque várias threads (uma por cliente) mexem no mapa ao mesmo tempo
    private final Map<String, Sessao> porToken = new ConcurrentHashMap<>();

    /** Abre uma sessão para o usuário (nome, email, funcao, matricula?) e devolve o token. */
    public String abrir(Document usuario) {
        // 32 bytes aleatórios = impossível de adivinhar; Base64 "url" para virar texto sem / e +
        byte[] bytes = new byte[32];
        ALEATORIO.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        porToken.put(token, new Sessao(usuario, System.currentTimeMillis() + DURACAO_MS));
        return token;
    }

    /** Usuário dono do token, ou null se o token não existe ou expirou. */
    public Document usuario(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        Sessao sessao = porToken.get(token);
        if (sessao == null) {
            return null;
        }
        if (System.currentTimeMillis() > sessao.expiraEm) {
            porToken.remove(token);
            return null;
        }
        return sessao.usuario;
    }

    /** Encerra todas as sessões de uma pessoa (ex.: acesso removido ou senha redefinida), menos {@code exceto}. */
    public void encerrarDoUsuario(String email, String exceto) {
        porToken.entrySet().removeIf(e -> !e.getKey().equals(exceto)
                && email.equals(e.getValue().usuario.getString("email")));
    }

    /** Atualiza o nome guardado nas sessões abertas da pessoa (depois de trocar em Minha conta). */
    public void atualizarNome(String email, String nome) {
        for (Sessao s : porToken.values()) {
            if (email.equals(s.usuario.getString("email"))) {
                s.usuario.put("nome", nome);
            }
        }
    }

    public void encerrar(String token) {
        if (token != null) {
            porToken.remove(token);
        }
    }

    private static final class Sessao {
        final Document usuario;
        final long expiraEm;

        Sessao(Document usuario, long expiraEm) {
            this.usuario = usuario;
            this.expiraEm = expiraEm;
        }
    }
}
