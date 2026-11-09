package br.puc.evasao.servidor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/**
 * Apaga e recria o banco com os dados de exemplo de dados/exemplo.csv.
 * Importa mês a mês, em ordem, para que os alertas de mudança de nível sejam gerados
 * como aconteceriam no uso real.
 */
public class PopularBanco {

    public static void main(String[] args) throws IOException {
        Path arquivo = Paths.get(args.length > 0 ? args[0] : "dados/exemplo.csv");
        List<String> linhas = Files.readAllLines(arquivo, StandardCharsets.UTF_8);
        String cabecalho = linhas.get(0);
        int colData = List.of(cabecalho.split(",")).indexOf("data");

        TreeSet<String> datas = new TreeSet<>();
        for (String l : linhas.subList(1, linhas.size())) {
            if (!l.isBlank()) datas.add(l.split(",")[colData]);
        }

        Banco banco = new Banco();
        banco.getDatabase().getCollection("alunos").drop();
        banco.getDatabase().getCollection("alertas").drop();

        AlunoRepositorio alunos = new AlunoRepositorio(banco);
        AlertaRepositorio alertas = new AlertaRepositorio(banco);
        ImportadorCsv importador = new ImportadorCsv(alunos, alertas);

        for (String data : datas) {
            List<String> doMes = new ArrayList<>();
            doMes.add(cabecalho);
            for (String l : linhas.subList(1, linhas.size())) {
                if (!l.isBlank() && l.split(",")[colData].equals(data)) doMes.add(l);
            }
            importador.importar(String.join("\n", doMes));
            System.out.println("[Popular] Importado período " + data + " (" + (doMes.size() - 1) + " registros)");
        }

        System.out.println("[Popular] Alunos: " + alunos.listar(null, null).size()
                + " | Alertas: " + alertas.listar().size());
        banco.fechar();
    }
}
