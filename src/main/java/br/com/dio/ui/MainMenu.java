package br.com.dio.ui;

import br.com.dio.persistence.entity.BoardColumnEntity;
import br.com.dio.persistence.entity.BoardColumnKindEnum;
import br.com.dio.persistence.entity.BoardEntity;
import br.com.dio.service.BoardQueryService;
import br.com.dio.service.BoardService;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import static br.com.dio.persistence.config.ConnectionConfig.getConnection;
import static br.com.dio.persistence.entity.BoardColumnKindEnum.*;

public class MainMenu {

    private final Scanner scanner = new Scanner(System.in).useDelimiter("\n");

    public void execute() throws SQLException {
        System.out.println("Bem-vindo ao gerenciador de boards!");
        while (true) {
            showMenu();
            int option = readInt("Escolha a opção desejada: ");

            switch (option) {
                case 1 -> createBoard();
                case 2 -> selectBoard();
                case 3 -> deleteBoard();
                case 4 -> {
                    System.out.println("Saindo do sistema. Até logo!");
                    System.exit(0);
                }
                default -> System.out.println("Opção inválida! Informe uma opção válida.");
            }
        }
    }

    private void showMenu() {
        System.out.println("\n=== Menu Principal ===");
        System.out.println("1 - Criar um novo board");
        System.out.println("2 - Selecionar um board existente");
        System.out.println("3 - Excluir um board");
        System.out.println("4 - Sair");
    }

    // ===================== MÉTODOS DE LEITURA =====================
    private int readInt(String prompt) {
        System.out.print(prompt);
        while (!scanner.hasNextInt()) {
            System.out.print("Entrada inválida! Digite um número: ");
            scanner.next();
        }
        int value = scanner.nextInt();
        scanner.nextLine(); // consome o \n
        return value;
    }

    private long readLong(String prompt) {
        System.out.print(prompt);
        while (!scanner.hasNextLong()) {
            System.out.print("Entrada inválida! Digite um número válido: ");
            scanner.next();
        }
        long value = scanner.nextLong();
        scanner.nextLine(); // consome o \n
        return value;
    }

    private String readString(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine();
    }

    // ===================== MÉTODOS DE AÇÃO =====================
    private void createBoard() throws SQLException {
        var entity = new BoardEntity();
        entity.setName(readString("Informe o nome do board: "));

        int additionalColumns = readInt("Colunas adicionais além das 3 padrões? Digite 0 se não houver: ");
        List<BoardColumnEntity> columns = new ArrayList<>();

        // Coluna inicial
        columns.add(createColumn(readString("Nome da coluna inicial: "), INITIAL, 0));

        // Colunas pendentes
        for (int i = 0; i < additionalColumns; i++) {
            columns.add(createColumn(readString("Nome da coluna pendente #" + (i+1) + ": "), PENDING, i + 1));
        }

        // Coluna final
        columns.add(createColumn(readString("Nome da coluna final: "), FINAL, additionalColumns + 1));

        // Coluna de cancelamento
        columns.add(createColumn(readString("Nome da coluna de cancelamento: "), CANCEL, additionalColumns + 2));

        entity.setBoardColumns(columns);

        try (var connection = getConnection()) {
            new BoardService(connection).insert(entity);
            System.out.println("Board criado com sucesso!");
        }
    }

    private void selectBoard() throws SQLException {
        long id = readLong("Informe o ID do board que deseja selecionar: ");
        try (var connection = getConnection()) {
            var queryService = new BoardQueryService(connection);
            var optional = queryService.findById(id);
            optional.ifPresentOrElse(
                    b -> new BoardMenu(b).execute(),
                    () -> System.out.printf("Não foi encontrado um board com ID %s\n", id)
            );
        }
    }

    private void deleteBoard() throws SQLException {
        long id = readLong("Informe o ID do board que será excluído: ");
        try (var connection = getConnection()) {
            var service = new BoardService(connection);
            if (service.delete(id)) {
                System.out.printf("O board %s foi excluído com sucesso!\n", id);
            } else {
                System.out.printf("Não foi encontrado um board com ID %s\n", id);
            }
        }
    }

    private BoardColumnEntity createColumn(String name, BoardColumnKindEnum kind, int order) {
        var column = new BoardColumnEntity();
        column.setName(name);
        column.setKind(kind);
        column.setOrder(order);
        return column;
    }
}
