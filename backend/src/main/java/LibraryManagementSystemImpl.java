import entities.Book;
import entities.Borrow;
import entities.Card;
import queries.*;
import utils.DBInitializer;
import utils.DatabaseConnector;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LibraryManagementSystemImpl implements LibraryManagementSystem {

    private static final Logger LOGGER = Logger.getLogger(LibraryManagementSystemImpl.class.getName());
    private final DatabaseConnector connector;

    public LibraryManagementSystemImpl(DatabaseConnector connector) {
        LOGGER.setLevel(Level.INFO);
        LOGGER.setUseParentHandlers(true);
//        Handler consoleHandler = new ConsoleHandler();
//        consoleHandler.setFormatter(new Formatter() {
//            @Override
//            public String format(LogRecord record) {
//                return String.format("%4$s: %5$s [%1$tc]%n", record.getMillis(), null, record.getLoggerName(), record.getLevel(), record.getMessage());
//            }
//        });
//        LOGGER.addHandler(consoleHandler);
        LOGGER.log(Level.INFO, "LibraryManagementSystemImpl created");
        this.connector = connector;
    }

    @Override
    public ApiResult storeBook(Book book) {
        Connection conn = connector.getConn();
        try {
            // if book exists, return error
            PreparedStatement stmt = conn.prepareStatement("SELECT * FROM book WHERE category = ? AND title = ? AND press = ? AND publish_year = ? AND author = ?");
            stmt.setString(1, book.getCategory());
            stmt.setString(2, book.getTitle());
            stmt.setString(3, book.getPress());
            stmt.setInt(4, book.getPublishYear());
            stmt.setString(5, book.getAuthor());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new ApiResult(false, "Book already exists");
            }
            // insert book
            stmt = conn.prepareStatement("INSERT INTO book (book_id, category, title, press, publish_year, author, price, stock) VALUES (?, ?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
            stmt.setInt(1, book.getBookId());
            stmt.setString(2, book.getCategory());
            stmt.setString(3, book.getTitle());
            stmt.setString(4, book.getPress());
            stmt.setInt(5, book.getPublishYear());
            stmt.setString(6, book.getAuthor());
            stmt.setDouble(7, book.getPrice());
            stmt.setInt(8, book.getStock());
            stmt.executeUpdate();
            rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                book.setBookId(rs.getInt(1));
            }
            commit(conn);
        } catch (Exception e) {
            rollback(conn);
            LOGGER.log(Level.SEVERE, e.getMessage());
            e.printStackTrace();
            return new ApiResult(false, e.getMessage());
        }
        return new ApiResult(true, null);
    }

    @Override
    public ApiResult incBookStock(int bookId, int deltaStock) {
        Connection conn = connector.getConn();
        try {
            // make sure stock is always non-negative
            PreparedStatement stmt = conn.prepareStatement("SELECT stock FROM book WHERE book_id = ?");
            stmt.setInt(1, bookId);
            ResultSet rs = stmt.executeQuery();
            if (!rs.next()) {
                return new ApiResult(false, "Book not found");
            }
            int stock = rs.getInt("stock");
            if (stock + deltaStock < 0) {
                return new ApiResult(false, "Stock will be negative");
            }
            // update stock
            stmt = conn.prepareStatement("UPDATE book SET stock = stock + ? WHERE book_id = ?");
            stmt.setInt(1, deltaStock);
            stmt.setInt(2, bookId);
            stmt.executeUpdate();
            commit(conn);
        } catch (Exception e) {
            rollback(conn);
            LOGGER.log(Level.SEVERE, e.getMessage());
            e.printStackTrace();
            return new ApiResult(false, e.getMessage());
        }
        return new ApiResult(true, null);
    }

    @Override
    public ApiResult storeBook(List<Book> books) {
        Connection conn = connector.getConn();
        try {
            PreparedStatement stmt = conn.prepareStatement("INSERT INTO book (category, title, press, publish_year, author, price, stock) VALUES (?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
            for (Book book : books) {
                stmt.setString(1, book.getCategory());
                stmt.setString(2, book.getTitle());
                stmt.setString(3, book.getPress());
                stmt.setInt(4, book.getPublishYear());
                stmt.setString(5, book.getAuthor());
                stmt.setDouble(6, book.getPrice());
                stmt.setInt(7, book.getStock());
                stmt.addBatch();
            }
            stmt.executeBatch();
            ResultSet rs = stmt.getGeneratedKeys();
            for (Book book : books) {
                if (rs.next()) {
                    book.setBookId(rs.getInt(1));
                }
            }
            commit(conn);
        } catch (Exception e) {
            rollback(conn);
            LOGGER.log(Level.SEVERE, e.getMessage());
            e.printStackTrace();
            return new ApiResult(false, e.getMessage());
        }
        return new ApiResult(true, null);
    }

    @Override
    public ApiResult removeBook(int bookId) {
        Connection conn = connector.getConn();
        try {
            // if book not exists, return error
            PreparedStatement stmt = conn.prepareStatement("SELECT * FROM book WHERE book_id = ?");
            stmt.setInt(1, bookId);
            ResultSet rs = stmt.executeQuery();
            if (!rs.next()) {
                return new ApiResult(false, "Book not found");
            }
            // if the book is borrowed and not returned, return error
            stmt = conn.prepareStatement("SELECT * FROM borrow WHERE book_id = ? AND return_time = 0");
            stmt.setInt(1, bookId);
            rs = stmt.executeQuery();
            if (rs.next()) {
                return new ApiResult(false, "Book is borrowed");
            }
            // delete book
            stmt = conn.prepareStatement("DELETE FROM book WHERE book_id = ?");
            stmt.setInt(1, bookId);
            stmt.executeUpdate();
            commit(conn);
        } catch (Exception e) {
            rollback(conn);
            LOGGER.log(Level.SEVERE, e.getMessage());
            e.printStackTrace();
            return new ApiResult(false, e.getMessage());
        }
        return new ApiResult(true, null);
    }

    @Override
    public ApiResult modifyBookInfo(Book book) {
        Connection conn = connector.getConn();
        try {
            PreparedStatement stmt = conn.prepareStatement("UPDATE book SET category = ?, title = ?, press = ?, publish_year = ?, author = ?, price = ? WHERE book_id = ?");
            stmt.setString(1, book.getCategory());
            stmt.setString(2, book.getTitle());
            stmt.setString(3, book.getPress());
            stmt.setInt(4, book.getPublishYear());
            stmt.setString(5, book.getAuthor());
            stmt.setDouble(6, book.getPrice());
            stmt.setInt(7, book.getBookId());
            stmt.executeUpdate();
            commit(conn);
        } catch (Exception e) {
            rollback(conn);
            LOGGER.log(Level.SEVERE, e.getMessage());
            e.printStackTrace();
            return new ApiResult(false, e.getMessage());
        }
        return new ApiResult(true, null);
    }

    @Override
    public ApiResult queryBook(BookQueryConditions conditions) {
        Connection conn = connector.getConn();
        try {
            StringBuilder sql = new StringBuilder("SELECT * FROM book WHERE 1 = 1");
            List<Object> parameters = new ArrayList<>();
            if (conditions.getCategory() != null) {
                sql.append(" AND category = ?");
                parameters.add(conditions.getCategory());
            }
// fuzzy match
            if (conditions.getTitle() != null) {
                sql.append(" AND title LIKE ?");
                parameters.add("%" + conditions.getTitle() + "%");
            }
// fuzzy match
            if (conditions.getPress() != null) {
                sql.append(" AND press LIKE ?");
                parameters.add("%" + conditions.getPress() + "%");
            }
            if (conditions.getMinPublishYear() != null) {
                sql.append(" AND publish_year >= ?");
                parameters.add(conditions.getMinPublishYear());
            }
            if (conditions.getMaxPublishYear() != null) {
                sql.append(" AND publish_year <= ?");
                parameters.add(conditions.getMaxPublishYear());
            }
// fuzzy match
            if (conditions.getAuthor() != null) {
                sql.append(" AND author LIKE ?");
                parameters.add("%" + conditions.getAuthor() + "%");
            }
            if (conditions.getMinPrice() != null) {
                sql.append(" AND price >= ?");
                parameters.add(conditions.getMinPrice());
            }
            if (conditions.getMaxPrice() != null) {
                sql.append(" AND price <= ?");
                parameters.add(conditions.getMaxPrice());
            }
            sql.append(" ORDER BY ").append(conditions.getSortBy().getValue()).append(" ").append(conditions.getSortOrder().getValue());
// default sort by book_id
            if (conditions.getSortBy() != Book.SortColumn.BOOK_ID) {
                sql.append(", book_id ASC");
            }
            PreparedStatement stmt = conn.prepareStatement(sql.toString());
            for (int i = 0; i < parameters.size(); i++) {
                stmt.setObject(i + 1, parameters.get(i));
            }
            ResultSet rs = stmt.executeQuery();
            List<Book> books = new ArrayList<>();
            while (rs.next()) {
                Book book = new Book();
                book.setBookId(rs.getInt("book_id"));
                book.setCategory(rs.getString("category"));
                book.setTitle(rs.getString("title"));
                book.setPress(rs.getString("press"));
                book.setPublishYear(rs.getInt("publish_year"));
                book.setAuthor(rs.getString("author"));
                book.setPrice(rs.getDouble("price"));
                book.setStock(rs.getInt("stock"));
                books.add(book);
            }
            BookQueryResults results = new BookQueryResults(books);
            return new ApiResult(true, results);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, e.getMessage());
            e.printStackTrace();
            return new ApiResult(false, e.getMessage());
        }
    }

    @Override
    public ApiResult borrowBook(Borrow borrow) {
        Connection conn = connector.getConn();
        // set serializable isolation level
        try {
            conn.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            // if user has borrowed the book, return error
            PreparedStatement stmt = conn.prepareStatement("SELECT * FROM borrow WHERE card_id = ? AND book_id = ? AND return_time = 0");
            stmt.setInt(1, borrow.getCardId());
            stmt.setInt(2, borrow.getBookId());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new ApiResult(false, "Book already borrowed");
            }
            // if book is out of stock, return error
            stmt = conn.prepareStatement("SELECT stock FROM book WHERE book_id = ?");
            stmt.setInt(1, borrow.getBookId());
            rs = stmt.executeQuery();
            if (!rs.next()) {
                return new ApiResult(false, "Book not found");
            }
            int stock = rs.getInt("stock");
            if (stock == 0) {
                return new ApiResult(false, "Book out of stock");
            }
            // borrow book
            stmt = conn.prepareStatement("INSERT INTO borrow (card_id, book_id, borrow_time) VALUES (?, ?, ?)");
            stmt.setInt(1, borrow.getCardId());
            stmt.setInt(2, borrow.getBookId());
            stmt.setLong(3, borrow.getBorrowTime());
            stmt.executeUpdate();
            // decrease stock
            stmt = conn.prepareStatement("UPDATE book SET stock = stock - 1 WHERE book_id = ?");
            stmt.setInt(1, borrow.getBookId());
            stmt.executeUpdate();
            commit(conn);
        } catch (Exception e) {
            rollback(conn);
            LOGGER.log(Level.SEVERE, e.getMessage());
            e.printStackTrace();
            return new ApiResult(false, e.getMessage());
        }
        return new ApiResult(true, null);
    }

    @Override
    public ApiResult returnBook(Borrow borrow) {
        Connection conn = connector.getConn();
        // if user has not borrowed the book, return error
        try {
            PreparedStatement stmt = conn.prepareStatement("SELECT * FROM borrow WHERE card_id = ? AND book_id = ? AND borrow_time = ? AND return_time = 0");
            stmt.setInt(1, borrow.getCardId());
            stmt.setInt(2, borrow.getBookId());
            stmt.setLong(3, borrow.getBorrowTime());
            ResultSet rs = stmt.executeQuery();
            if (!rs.next()) {
                return new ApiResult(false, "Book not borrowed");
            }
            // return book
            stmt = conn.prepareStatement("UPDATE borrow SET return_time = ? WHERE card_id = ? AND book_id = ? AND borrow_time = ?");
            stmt.setLong(1, borrow.getReturnTime());
            stmt.setInt(2, borrow.getCardId());
            stmt.setInt(3, borrow.getBookId());
            stmt.setLong(4, borrow.getBorrowTime());
            stmt.executeUpdate();
            // increase stock
            stmt = conn.prepareStatement("UPDATE book SET stock = stock + 1 WHERE book_id = ?");
            stmt.setInt(1, borrow.getBookId());
            stmt.executeUpdate();
            commit(conn);
        } catch (Exception e) {
            rollback(conn);
            LOGGER.log(Level.SEVERE, e.getMessage());
            e.printStackTrace();
            return new ApiResult(false, e.getMessage());
        }
        return new ApiResult(true, null);
    }

    @Override
    public ApiResult showBorrowHistory(int cardId) {
        Connection conn = connector.getConn();
        try {
            // query borrow history joined with book
            PreparedStatement stmt = conn.prepareStatement("SELECT * FROM borrow JOIN book ON borrow.book_id = book.book_id WHERE card_id = ? ORDER BY borrow_time DESC, book.book_id ASC");
            stmt.setInt(1, cardId);
            ResultSet rs = stmt.executeQuery();
            List<BorrowHistories.Item> borrows = new ArrayList<>();
            while (rs.next()) {
                Book book = new Book(rs.getString("category"), rs.getString("title"), rs.getString("press"), rs.getInt("publish_year"), rs.getString("author"), rs.getDouble("price"), rs.getInt("stock"));
                book.setBookId(rs.getInt("book_id"));
                Borrow borrow = new Borrow(rs.getInt("book_id"), rs.getInt("card_id"));
                borrow.setBorrowTime(rs.getLong("borrow_time"));
                borrow.setReturnTime(rs.getLong("return_time"));
                BorrowHistories.Item i = new BorrowHistories.Item(cardId, book, borrow);
                borrows.add(i);
            }
            BorrowHistories histories = new BorrowHistories(borrows);
            return new ApiResult(true, histories);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, e.getMessage());
            e.printStackTrace();
            return new ApiResult(false, e.getMessage());
        }
    }

    @Override
    public ApiResult registerCard(Card card) {
        Connection conn = connector.getConn();
        // if card exists, return error
        try {
            PreparedStatement stmt = conn.prepareStatement("SELECT * FROM card WHERE name = ? AND department = ? AND type = ?");
            stmt.setString(1, card.getName());
            stmt.setString(2, card.getDepartment());
            stmt.setString(3, String.valueOf(card.getType()));
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new ApiResult(false, "Card already exists");
            }
            // insert card
            stmt = conn.prepareStatement("INSERT INTO card (name, department, type) VALUES (?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
            stmt.setString(1, card.getName());
            stmt.setString(2, card.getDepartment());
            stmt.setString(3, card.getType().getStr());
            stmt.executeUpdate();
            rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                card.setCardId(rs.getInt(1));
            }
            commit(conn);
        } catch (Exception e) {
            rollback(conn);
            LOGGER.log(Level.SEVERE, e.getMessage());
            e.printStackTrace();
            return new ApiResult(false, e.getMessage());
        }
        return new ApiResult(true, null);
    }

    @Override
    public ApiResult modifyCardInfo(Card card) {
        Connection conn = connector.getConn();
        try {
            PreparedStatement stmt = conn.prepareStatement("UPDATE card SET name = ?, department = ?, type = ? WHERE card_id = ?");
            stmt.setString(1, card.getName());
            stmt.setString(2, card.getDepartment());
            stmt.setString(3, card.getType().getStr());
            stmt.setInt(4, card.getCardId());
            stmt.executeUpdate();
            commit(conn);
        } catch (Exception e) {
            rollback(conn);
            LOGGER.log(Level.SEVERE, e.getMessage());
            e.printStackTrace();
            return new ApiResult(false, e.getMessage());
        }
        return new ApiResult(true, null);
    }

    @Override
    public ApiResult removeCard(int cardId) {
        Connection conn = connector.getConn();
        try {
            // if card not exists, return error
            PreparedStatement stmt = conn.prepareStatement("SELECT * FROM card WHERE card_id = ?");
            stmt.setInt(1, cardId);
            ResultSet rs = stmt.executeQuery();
            if (!rs.next()) {
                return new ApiResult(false, "Card not found");
            }
            // if the card has borrowed books, return error
            stmt = conn.prepareStatement("SELECT * FROM borrow WHERE card_id = ? AND return_time = 0");
            stmt.setInt(1, cardId);
            rs = stmt.executeQuery();
            if (rs.next()) {
                return new ApiResult(false, "Card has borrowed books");
            }
            // delete card
            stmt = conn.prepareStatement("DELETE FROM card WHERE card_id = ?");
            stmt.setInt(1, cardId);
            stmt.executeUpdate();
            commit(conn);
        } catch (Exception e) {
            rollback(conn);
            LOGGER.log(Level.SEVERE, e.getMessage());
            e.printStackTrace();
            return new ApiResult(false, e.getMessage());
        }
        return new ApiResult(true, null);
    }

    @Override
    public ApiResult showCards() {
        Connection conn = connector.getConn();
        try {
            PreparedStatement stmt = conn.prepareStatement("SELECT * FROM card ORDER BY card_id");
            ResultSet rs = stmt.executeQuery();
            List<Card> cards = new ArrayList<>();
            while (rs.next()) {
                Card card = new Card();
                card.setCardId(rs.getInt("card_id"));
                card.setName(rs.getString("name"));
                card.setDepartment(rs.getString("department"));
                char type = rs.getString("type").charAt(0);
                if (type == 'T') {
                    card.setType(Card.CardType.Teacher);
                } else if (type == 'S') {
                    card.setType(Card.CardType.Student);
                } else {
                    //error
                    throw new Exception("Invalid card type");
                }
                cards.add(card);
            }
            CardList cardList = new CardList(cards);
            return new ApiResult(true, cardList);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, e.getMessage());
            e.printStackTrace();
            return new ApiResult(false, e.getMessage());
        }
    }

    @Override
    public ApiResult resetDatabase() {
        Connection conn = connector.getConn();
        try {
            Statement stmt = conn.createStatement();
            DBInitializer initializer = connector.getConf().getType().getDbInitializer();
            stmt.addBatch(initializer.sqlDropBorrow());
            stmt.addBatch(initializer.sqlDropBook());
            stmt.addBatch(initializer.sqlDropCard());
            stmt.addBatch(initializer.sqlCreateCard());
            stmt.addBatch(initializer.sqlCreateBook());
            stmt.addBatch(initializer.sqlCreateBorrow());
            stmt.executeBatch();
            commit(conn);
        } catch (Exception e) {
            rollback(conn);
            LOGGER.log(Level.SEVERE, e.getMessage());
            e.printStackTrace();
            return new ApiResult(false, e.getMessage());
        }
        return new ApiResult(true, null);
    }

    private void rollback(Connection conn) {
        try {
            conn.rollback();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, e.getMessage());
            e.printStackTrace();
        }
    }

    private void commit(Connection conn) {
        try {
            conn.commit();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, e.getMessage());
            e.printStackTrace();
        }
    }

}
