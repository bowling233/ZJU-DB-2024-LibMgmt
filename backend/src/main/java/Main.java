import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import entities.Book;
import entities.Borrow;
import entities.Card;
import queries.*;
import utils.ConnectConfig;
import utils.DatabaseConnector;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.*;
import java.util.stream.Collectors;

public class Main {

    private static final Logger log = Logger.getLogger(Main.class.getName());

    private static DatabaseConnector connector;
    private static LibraryManagementSystem library;

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        log.setLevel(Level.ALL);
        log.setUseParentHandlers(false);
        Handler consoleHandler = new ConsoleHandler();
        consoleHandler.setFormatter(new Formatter() {
            @Override
            public String format(LogRecord record) {
                return String.format("%4$s: %5$s [%1$tc]%n", record.getMillis(), null, record.getLoggerName(), record.getLevel(), record.getMessage());
            }
        });
        log.addHandler(consoleHandler);

        System.out.println("ZJUDB LibMgmt Backend Version Final.");
        System.out.println("Editor: bowling");

        ConnectConfig conf = new ConnectConfig();
        connector = new DatabaseConnector(conf);
        boolean connStatus = connector.connect();
        if (!connStatus) {
            log.log(Level.SEVERE, "[DB] Failed to connect database.");
            System.exit(1);
        }
        log.log(Level.INFO, "[DB] Success to connect database.");
        library = new LibraryManagementSystemImpl(connector);

        HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
        server.createContext("/cards", new CardHandler());
        server.createContext("/books", new BookHandler());
        server.start();
        log.log(Level.INFO, "[Web] Server started on port 8000.");

        Runtime.getRuntime().addShutdownHook(new Thread() {

            @Override
            public void run() {
                server.stop(0);
                System.out.println("Shutting down...");
                try {
                    connector.release();
                    System.out.println("[DB] Success to disconnect database.");
                } catch (Exception e) {
                    System.out.println("[DB] Failed to disconnect database.");
                    e.printStackTrace();
                }
            }
        });
    }

    private static String getQueryParam(Map<String, String> queryParams, String argument) {
        return queryParams.getOrDefault(argument, null);
    }

    private static int getQueryParamAsInt(Map<String, String> queryParams, String argument) {
        String param = getQueryParam(queryParams, argument);
        return param == null ? 0 : Integer.parseInt(param);
    }

    private static double getQueryParamAsDouble(Map<String, String> queryParams, String argument) {
        String param = getQueryParam(queryParams, argument);
        return param == null ? 0 : Double.parseDouble(param);
    }

    private static void makeResponse(HttpExchange exchange, ApiResult ar) throws IOException {
        exchange.sendResponseHeaders(ar.ok ? 200 : 500, 0);
        exchange.getResponseHeaders().set("Content-Type", "text/plain");
        OutputStream outputStream = exchange.getResponseBody();
        outputStream.write(ar.ok ? "".getBytes() : ar.message.getBytes());
        outputStream.close();
    }

    static class CardHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String requestMethod = exchange.getRequestMethod();
            URI uri = exchange.getRequestURI();
            String path = uri.getPath();
            log.log(Level.INFO, "[Web] Request: " + path + " " + requestMethod);
            String[] pathParts = path.split("/");
            switch (pathParts.length) {
                case 2: // /cards
                    switch (requestMethod) {
                        case "GET":
                            cardsGet(exchange);
                            break;
                        case "POST":
                            cardsPost(exchange);
                            break;
                        case "OPTIONS":// preflight response
                            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST");
                            exchange.sendResponseHeaders(204, -1);
                            break;

                        default:
                            exchange.sendResponseHeaders(405, -1);
                            break;
                    }
                    break;
                case 3: // /cards/{cardId}
                    try {
                        Integer.parseInt(pathParts[2]);
                    } catch (NumberFormatException e) {
                        exchange.sendResponseHeaders(404, -1);
                        return;
                    }
                    switch (requestMethod) {
                        case "PUT":
                            cardsIdPut(exchange, pathParts);
                            break;
                        case "DELETE":
                            cardsIdDelete(exchange, pathParts);
                            break;
                        case "OPTIONS":// preflight response
                            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "PUT, DELETE");
                            exchange.sendResponseHeaders(204, -1);
                            break;
                        default:
                            exchange.sendResponseHeaders(405, -1);
                            break;
                    }
                    break;
                case 4: // /cards/{cardId}/borrows
                    if (!pathParts[3].equals("borrows")) {
                        exchange.sendResponseHeaders(404, -1);
                        return;
                    }
                    try {
                        Integer.parseInt(pathParts[2]);
                    } catch (NumberFormatException e) {
                        exchange.sendResponseHeaders(404, -1);
                        return;
                    }
                    switch (requestMethod) {
                        case "GET":
                            cardsIdBorrowsGet(exchange, pathParts);
                            break;
                        case "POST":
                            cardsIdBorrowsPost(exchange, pathParts);
                            break;
                        case "DELETE":
                            cardsIdBorrowsDelete(exchange, pathParts);
                            break;
                        case "OPTIONS":// preflight response
                            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, DELETE");
                            exchange.sendResponseHeaders(204, -1);
                            break;
                        default:
                            exchange.sendResponseHeaders(405, -1);
                            break;
                    }
                    break;
                default:
                    exchange.sendResponseHeaders(404, -1);
                    break;
            }
        }

        private void cardsIdDelete(HttpExchange exchange, String[] pathParts) throws IOException {
            log.log(Level.INFO, "[Web] /cards/{id}: DELETE.");

            int cardId = Integer.parseInt(pathParts[2]);
            ApiResult ar = library.removeCard(cardId);

            makeResponse(exchange, ar);
        }

        private void cardsGet(HttpExchange exchange) throws IOException {
            log.log(Level.INFO, "[Web] /cards: GET.");

            ApiResult ar = library.showCards();

            if (!ar.ok) {
                makeResponse(exchange, ar);
                return;
            }

            CardList cardList = (CardList) ar.payload;
            JSONArray jsonArray = new JSONArray();
            List<Card> lc = cardList.getCards();
            for (Card c : lc) {
                jsonArray.add(JSONObject.parseObject(c.toJson()));
            }

            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, 0);
            OutputStream outputStream = exchange.getResponseBody();
            outputStream.write(jsonArray.toJSONString().getBytes());
            outputStream.close();
        }

        private void cardsIdPut(HttpExchange exchange, String[] pathParts) throws IOException {
            log.log(Level.INFO, "[Web] /cards/{id}: PUT.");

            String requestBody = new BufferedReader(new InputStreamReader(exchange.getRequestBody())).lines().collect(Collectors.joining("\n"));
            Card card = new Card(JSONObject.parseObject(requestBody));
            ApiResult ar = library.modifyCardInfo(card);

            makeResponse(exchange, ar);
        }

        private void cardsPost(HttpExchange exchange) throws IOException {
            log.log(Level.INFO, "[Web] /cards: POST.");

            String requestBody = new BufferedReader(new InputStreamReader(exchange.getRequestBody())).lines().collect(Collectors.joining("\n"));
            Card card = new Card(JSONObject.parseObject(requestBody));
            ApiResult ar = library.registerCard(card);

            makeResponse(exchange, ar);
        }

        private void cardsIdBorrowsGet(HttpExchange exchange, String[] partPaths) throws IOException {
            log.log(Level.INFO, "[Web] /cards/{id}/borrows: GET.");

            int cardId = Integer.parseInt(partPaths[2]);
            ApiResult ar = library.showBorrowHistory(cardId);
            if (!ar.ok) {
                makeResponse(exchange, ar);
                return;
            }

            JSONArray jsonArray = new JSONArray();
            BorrowHistories bh = (BorrowHistories) ar.payload;
            List<BorrowHistories.Item> items = bh.getItems();
            for (BorrowHistories.Item item : items) {
                jsonArray.add(JSONObject.parseObject(item.toJson()));
            }

            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, 0);
            OutputStream outputStream = exchange.getResponseBody();
            outputStream.write(jsonArray.toJSONString().getBytes());
            outputStream.close();
        }

        private void cardsIdBorrowsDelete(HttpExchange exchange, String[] pathParts) throws IOException {
            log.log(Level.INFO, "[Web] /cards/{id}/borrows: DELETE.");

            String s = new BufferedReader(new InputStreamReader(exchange.getRequestBody())).lines().collect(Collectors.joining("\n"));
            JSONObject jsonObject = JSONObject.parseObject(s);

            Borrow borrow = new Borrow(jsonObject);
            ApiResult ar = library.returnBook(borrow);

            makeResponse(exchange, ar);
        }

        private void cardsIdBorrowsPost(HttpExchange exchange, String[] pathParts) throws IOException {
            log.log(Level.INFO, "[Web] /cards/{id}/borrows: POST.");

            String s = new BufferedReader(new InputStreamReader(exchange.getRequestBody())).lines().collect(Collectors.joining("\n"));

            Borrow borrow = new Borrow(JSONObject.parseObject(s));
            borrow.setCardId(Integer.parseInt(pathParts[2]));
            ApiResult ar = library.borrowBook(borrow);

            makeResponse(exchange, ar);
        }
    }

    static class BookHandler implements HttpHandler {

        private static Map<String, String> parseQueryParams(String query) {
            Map<String, String> queryParams = new HashMap<>();
            String[] params = query.split("&");

            for (String param : params) {
                String[] keyValue = param.split("=");
                String key = keyValue[0];
                String value = keyValue.length > 1 ? keyValue[1] : null;
                queryParams.put(key, value);
            }

            return queryParams;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String requestMethod = exchange.getRequestMethod();
            URI uri = exchange.getRequestURI();
            String path = uri.getPath();
            log.log(Level.INFO, "[Web] Request: " + path + " " + requestMethod);
            String[] pathParts = path.split("/");
            switch (pathParts.length) {
                case 2: // /books
                    switch (requestMethod) {
                        case "GET":
                            booksGet(exchange);
                            break;
                        case "POST":
                            booksPost(exchange);
                            break;
                        case "OPTIONS":// preflight response
                            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST");
                            exchange.sendResponseHeaders(204, -1);
                            break;
                        default:
                            exchange.sendResponseHeaders(405, -1);
                            break;
                    }
                    break;
                case 3: // /books/{bookId}
                    try {
                        Integer.parseInt(pathParts[2]);
                    } catch (NumberFormatException e) {
                        exchange.sendResponseHeaders(404, -1);
                        return;
                    }
                    switch (requestMethod) {
                        case "PUT":
                            booksIdPut(exchange, pathParts);
                            break;
                        case "DELETE":
                            booksIdDelete(exchange, pathParts);
                            break;
                        case "OPTIONS":// preflight response
                            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "PUT, DELETE");
                            exchange.sendResponseHeaders(204, -1);
                            break;
                        default:
                            exchange.sendResponseHeaders(405, -1);
                            break;
                    }
                default:
                    exchange.sendResponseHeaders(404, -1);
                    break;
            }
        }

        private void booksGet(HttpExchange exchange) throws IOException {
            log.log(Level.INFO, "[Web] /books: GET.");

            String s = exchange.getRequestURI().getQuery();
            Map<String, String> queryParams = parseQueryParams(s);

            String category = getQueryParam(queryParams, "category");
            String title = getQueryParam(queryParams, "title");
            String press = getQueryParam(queryParams, "press");
            int minPublishYear = getQueryParamAsInt(queryParams, "minPublishYear");
            int maxPublishYear = getQueryParamAsInt(queryParams, "maxPublishYear");
            String author = getQueryParam(queryParams, "author");
            double minPrice = getQueryParamAsDouble(queryParams, "minPrice");
            double maxPrice = getQueryParamAsDouble(queryParams, "maxPrice");
            String sortBy = getQueryParam(queryParams, "sortBy");
            String sortOrder = getQueryParam(queryParams, "sortOrder");

            BookQueryConditions bqc = new BookQueryConditions();
            bqc.setAuthor(author);
            bqc.setCategory(category);
            if (maxPrice != 0) bqc.setMaxPrice(maxPrice);
            if (maxPublishYear != 0) bqc.setMaxPublishYear(maxPublishYear);
            if (minPrice != 0) bqc.setMinPrice(minPrice);
            if (minPublishYear != 0) bqc.setMinPublishYear(minPublishYear);
            bqc.setPress(press);
            if (sortBy != null) bqc.setSortBy(Book.SortColumn.fromString(sortBy.toUpperCase()));
            if (sortOrder != null) bqc.setSortOrder(SortOrder.fromString(sortOrder.toUpperCase()));
            bqc.setTitle(title);
            log.log(Level.INFO, "[DB] Query for book \n" + bqc);

            StringBuilder json = new StringBuilder("[");
            ApiResult ar = library.queryBook(bqc);
            if (!ar.ok) {
                makeResponse(exchange, ar);
                return;
            }
            BookQueryResults bqr = (BookQueryResults) ar.payload;
            List<Book> books = bqr.getResults();
            for (int i = 0; i < books.size(); i++) {
                Book book = books.get(i);
                json.append(book.toJson());
                if (i != books.size() - 1) {
                    json.append(",");
                }
            }
            json.append("]");

            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, 0);
            OutputStream outputStream = exchange.getResponseBody();
            outputStream.write(json.toString().getBytes());
            outputStream.close();
        }

        private void booksPost(HttpExchange exchange) throws IOException {
            log.log(Level.INFO, "[Web] /books: POST.");
            String requestBody = new BufferedReader(new InputStreamReader(exchange.getRequestBody())).lines().collect(Collectors.joining("\n"));
            JSONObject jsonObject = JSONObject.parseObject(requestBody);

            Book book = new Book();
            book.setCategory(jsonObject.getString("category"));
            book.setTitle(jsonObject.getString("title"));
            book.setPress(jsonObject.getString("press"));
            book.setAuthor(jsonObject.getString("author"));
            book.setPublishYear(jsonObject.getInteger("publishYear"));
            book.setStock(jsonObject.getInteger("stock"));
            book.setPrice(jsonObject.getDouble("price"));
            ApiResult ar = library.storeBook(book);

            makeResponse(exchange, ar);
        }

        private void booksIdPut(HttpExchange exchange, String[] pathParts) throws IOException {
            log.log(Level.INFO, "[Web] /books/{id}: POST.");
            String requestBody = new BufferedReader(new InputStreamReader(exchange.getRequestBody())).lines().collect(Collectors.joining("\n"));

            Book book = new Book(JSONObject.parseObject(requestBody));
            book.setBookId(Integer.parseInt(pathParts[2]));
            ApiResult ar = library.modifyBookInfo(book);
            makeResponse(exchange, ar);
        }

        private void booksIdDelete(HttpExchange exchange, String[] pathParts) throws IOException {
            log.log(Level.INFO, "[Web] /books/{id}: DELETE.");
            int bookId = Integer.parseInt(pathParts[2]);
            ApiResult ar = library.removeBook(bookId);
            makeResponse(exchange, ar);
        }
    }
}


