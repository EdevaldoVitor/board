package br.com.dio.ui;

import br.com.dio.dto.BoardColumnInfoDTO;
import br.com.dio.persistence.entity.BoardColumnEntity;
import br.com.dio.persistence.entity.BoardEntity;
import br.com.dio.persistence.entity.CardEntity;
import br.com.dio.service.BoardColumnQueryService;
import br.com.dio.service.BoardQueryService;
import br.com.dio.service.CardQueryService;
import br.com.dio.service.CardService;
import lombok.AllArgsConstructor;

import java.sql.SQLException;
import java.util.Scanner;

import static br.com.dio.persistence.config.ConnectionConfig.getConnection;

@AllArgsConstructor
public class BoardMenu {

    private final Scanner scanner = new Scanner(System.in).useDelimiter("\n");
    private final BoardEntity entity;

    public void execute() {
        try {
            System.out.printf("Bem-vindo ao board %s!\n", entity.getName());
            int option = -1;

            while (option != 10) {
                showMenu();
                option = readInt("Escolha uma opção: ");

                switch (option) {
                    case 1 -> createCard();
                    case 2 -> moveCardToNextColumn();
                    case 3 -> blockCard();
                    case 4 -> unblockCard();
                    case 5 -> cancelCard();
                    case 6 -> showBoard();
                    case 7 -> showColumn();
                    case 8 -> showCard();
                    case 9 -> System.out.println("Voltando para o menu anterior...");
                    case 10 -> {
                        System.out.println("Saindo do sistema. Até logo!");
                        System.exit(0);
                    }
                    default -> System.out.println("Opção inválida! Informe uma opção válida.");
                }
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
            System.exit(0);
        }
    }

    private void showMenu() {
        System.out.println("\n=== Menu do Board ===");
        System.out.println("1 - Criar um card");
        System.out.println("2 - Mover um card");
        System.out.println("3 - Bloquear um card");
        System.out.println("4 - Desbloquear um card");
        System.out.println("5 - Cancelar um card");
        System.out.println("6 - Ver board");
        System.out.println("7 - Ver coluna com cards");
        System.out.println("8 - Ver card");
        System.out.println("9 - Voltar para o menu anterior");
        System.out.println("10 - Sair");
    }

    private int readInt(String prompt) {
        System.out.print(prompt);
        while (!scanner.hasNextInt()) {
            System.out.print("Entrada inválida! Digite um número: ");
            scanner.next(); // descarta entrada inválida
        }
        int value = scanner.nextInt();
        scanner.nextLine(); // consome o \n restante
        return value;
    }

    private String readString(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine();
    }

    // ===== Métodos de ação =====
    private void createCard() throws SQLException {
        var card = new CardEntity();
        card.setTitle(readString("Informe o título do card: "));
        card.setDescription(readString("Informe a descrição do card: "));
        card.setBoardColumn(entity.getInitialColumn());

        try (var connection = getConnection()) {
            new CardService(connection).create(card);
            System.out.println("Card criado com sucesso!");
        }
    }

    private void moveCardToNextColumn() throws SQLException {
        long cardId = readInt("Informe o ID do card que deseja mover: ");
        var boardColumnsInfo = entity.getBoardColumns().stream()
                .map(bc -> new BoardColumnInfoDTO(bc.getId(), bc.getOrder(), bc.getKind()))
                .toList();

        try (var connection = getConnection()) {
            new CardService(connection).moveToNextColumn(cardId, boardColumnsInfo);
            System.out.println("Card movido com sucesso!");
        } catch (RuntimeException ex) {
            System.out.println("Erro: " + ex.getMessage());
        }
    }

    private void blockCard() throws SQLException {
        long cardId = readInt("Informe o ID do card que será bloqueado: ");
        String reason = readString("Informe o motivo do bloqueio: ");
        var boardColumnsInfo = entity.getBoardColumns().stream()
                .map(bc -> new BoardColumnInfoDTO(bc.getId(), bc.getOrder(), bc.getKind()))
                .toList();

        try (var connection = getConnection()) {
            new CardService(connection).block(cardId, reason, boardColumnsInfo);
            System.out.println("Card bloqueado com sucesso!");
        } catch (RuntimeException ex) {
            System.out.println("Erro: " + ex.getMessage());
        }
    }

    private void unblockCard() throws SQLException {
        long cardId = readInt("Informe o ID do card que será desbloqueado: ");
        String reason = readString("Informe o motivo do desbloqueio: ");

        try (var connection = getConnection()) {
            new CardService(connection).unblock(cardId, reason);
            System.out.println("Card desbloqueado com sucesso!");
        } catch (RuntimeException ex) {
            System.out.println("Erro: " + ex.getMessage());
        }
    }

    private void cancelCard() throws SQLException {
        long cardId = readInt("Informe o ID do card que deseja cancelar: ");
        var cancelColumn = entity.getCancelColumn();
        var boardColumnsInfo = entity.getBoardColumns().stream()
                .map(bc -> new BoardColumnInfoDTO(bc.getId(), bc.getOrder(), bc.getKind()))
                .toList();

        try (var connection = getConnection()) {
            new CardService(connection).cancel(cardId, cancelColumn.getId(), boardColumnsInfo);
            System.out.println("Card cancelado com sucesso!");
        } catch (RuntimeException ex) {
            System.out.println("Erro: " + ex.getMessage());
        }
    }

    private void showBoard() throws SQLException {
        try (var connection = getConnection()) {
            new BoardQueryService(connection).showBoardDetails(entity.getId())
                    .ifPresent(b -> {
                        System.out.printf("Board [%s - %s]\n", b.id(), b.name());
                        b.columns().forEach(c ->
                                System.out.printf("Coluna [%s] tipo: [%s] tem %s cards\n", c.name(), c.kind(), c.cardsAmount())
                        );
                    });
        }
    }

    private void showColumn() throws SQLException {
        var columnsIds = entity.getBoardColumns().stream().map(BoardColumnEntity::getId).toList();
        long selectedColumnId = -1;

        while (!columnsIds.contains(selectedColumnId)) {
            System.out.printf("Escolha uma coluna do board %s pelo ID:\n", entity.getName());
            entity.getBoardColumns().forEach(c -> System.out.printf("%s - %s [%s]\n", c.getId(), c.getName(), c.getKind()));
            selectedColumnId = readInt("ID da coluna: ");
        }

        try (var connection = getConnection()) {
            var column = new BoardColumnQueryService(connection).findById(selectedColumnId);
            column.ifPresent(co -> {
                System.out.printf("Coluna %s tipo %s\n", co.getName(), co.getKind());
                co.getCards().forEach(ca ->
                        System.out.printf("Card %s - %s\nDescrição: %s\n", ca.getId(), ca.getTitle(), ca.getDescription())
                );
            });
        }
    }

    private void showCard() throws SQLException {
        long cardId = readInt("Informe o ID do card que deseja visualizar: ");

        try (var connection = getConnection()) {
            new CardQueryService(connection).findById(cardId)
                    .ifPresentOrElse(
                            c -> {
                                System.out.printf("Card %s - %s\nDescrição: %s\n", c.id(), c.title(), c.description());
                                System.out.println(c.blocked() ? "Está bloqueado. Motivo: " + c.blockReason() : "Não está bloqueado");
                                System.out.printf("Já foi bloqueado %s vezes\n", c.blocksAmount());
                                System.out.printf("Está na coluna %s - %s\n", c.columnId(), c.columnName());
                            },
                            () -> System.out.printf("Não existe um card com o ID %s\n", cardId)
                    );
        }
    }
}
