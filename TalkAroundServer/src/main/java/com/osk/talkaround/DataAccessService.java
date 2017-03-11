package com.osk.talkaround;

import com.osk.talkaround.model.Answer;
import com.osk.talkaround.model.CustomLocation;
import com.osk.talkaround.model.Talk;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * Created by KOsinsky on 19.03.2016.
 */
public class DataAccessService {

    public final static String TALK_ID = "talkId";
    public final static String TALK_CREATIONDATE = "creationdate";
    public final static String TALK_TITLE = "talkTitle";
    public final static String TALK_TEXT = "talkText";
    public final static String TALK_LATITUDE = "talkLatitude";
    public final static String TALK_LONGITUDE = "talkLongitude";
    public final static String TALK_DISTANCE = "distance";
    public final static String ANSWER_TEXT = "answerText";

    private final static String TALK_TABLE_NAME = "talk";

    private static final String SELECT_AVAILABLE_TALKS = "SELECT talk.* FROM talk";
    private static final String INSERT_NEW_TALK_SQL =
            "INSERT INTO Talk(creationdate, title, text, longitude, latitude) " +
                    "VALUES(?,?,?,?,?)";
    private static final String INSERT_NEW_ANSWER_FOR_TALK_SQL =
            "INSERT INTO answer(talkid, ordernumber, answerdate, message) " +
                    "VALUES(?,?,?,?)";
    private static final String SELECT_TALK_BY_ID = "SELECT * FROM " + TALK_TABLE_NAME + " WHERE id = ?";
    private static final String SELECT_ANSWERS_BY_TALK_ID = "SELECT * FROM answer WHERE talkid = ?";

    private static final String ANSWER_ID = "id";
    private static final String ANSWER_ORDER_NUM = "ordernumber";
    private static final String ANSWER_TALK_ID = "talkid";
    private static final String ANSWER_DATE = "answerdate";
    private static final String ANSWER_MSG = "message";

    private static volatile List<Talk> talkList = new ArrayList<Talk>();

    private static BigInteger talkIdSeq = null;
    private static BigInteger answerIdSeq = null;

    // JDBC driver name and database URL
    static final String JDBC_DRIVER = "org.postgresql.Driver";
    static final String DB_URL = "jdbc:postgresql://localhost:5432/talkaroundbase";

    //  Database credentials
    static final String USER = "talkaround";
    static final String PASS = "talkaround";

    private static volatile DataAccessService instance;

    public static DataAccessService getInstance() {
        DataAccessService localInstance = instance;
        if (localInstance == null) {
            synchronized (DataAccessService.class) {
                localInstance = instance;
                if (localInstance == null) {
                    instance = localInstance = new DataAccessService();
                }
            }
        }
        return localInstance;
    }

    private DataAccessService() {
        try {
            Connection conn = openNewConnection();
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Connection openNewConnection() {
        try {
            Class.forName(JDBC_DRIVER);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        Connection connection = null;
        try {
            connection = DriverManager.getConnection(DB_URL, USER, PASS);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        System.out.println("Opened database successfully");
        return connection;
    }

    public InputStream createNewTalkInputStream(InputStream inputStream) throws IOException, ClassNotFoundException {
        return getInputStreamFromObject(createNewTalk(inputStream));
    }

    public InputStream getTalksInputStream() throws IOException {
        return getInputStreamFromObject(talkList);
    }

    public InputStream getAvailableTalks(Double longitude, Double latitude, Float distance) throws IOException {
        ArrayList<Talk> resultList = new ArrayList<Talk>();
        try {
            resultList = getTalksForLocation(longitude, latitude, distance);
        } catch (SQLException e) {
            e.printStackTrace();
        }
//        CustomLocation location = new CustomLocation(longitude, latitude);
//        for (Talk talk : talkList) {
//            Float distanceBetween = CustomLocationUtils.distanceTo(location, talk.getLocation());
//            System.out.println("Distance between user and talk " + talk.getTitle() + " is " + distanceBetween);
//            if (distanceBetween <= distance) {
//                talk.setDistance(distanceBetween);
//                resultList.add(talk);
//            }
//        }
        return getInputStreamFromObject(resultList);
    }

    private ArrayList<Talk> getTalksForLocation(Double longitude, Double latitude, Float distance) throws SQLException {
        Connection conn = openNewConnection();
        PreparedStatement statement = conn.prepareStatement(SELECT_AVAILABLE_TALKS);
//        String query = String.format("SELECT talk.*, " +
//                "(point("+longitude+","+latitude+") <@> point(talk.longitude, talk.latitude)) as distance FROM %s talk " +
//                "WHERE distance<" + distance, TALK_TABLE_NAME);
        System.out.println(statement.toString());
        ResultSet result = statement.executeQuery();
        ArrayList<Talk> talks = new ArrayList<Talk>();
        while (result.next()) {
            talks.add(createTalk(BigInteger.valueOf(result.getInt(1)),
                    result.getDate(2),
                    result.getString(3),
                    result.getString(4),
                    result.getDouble(5),
                    result.getDouble(6)));
        }
        return talks;
    }

    public InputStream getTalkByIdInputStream(String talkId) throws IOException, SQLException {
        return getInputStreamFromObject(getTalkById(talkId));
    }

    public synchronized InputStream addNewAnswerToTalkInputStream(InputStream inputStream) throws IOException, SQLException {
        return getInputStreamFromObject(addNewAnswerToTalk(inputStream));
    }

    private Talk createNewTalk(InputStream inputStream) throws IOException {
        Map<String, String> talkParams = getParamsFromInputStream(inputStream);
        // parse params
        String title = talkParams.get(TALK_TITLE);
        String text = talkParams.get(TALK_TEXT);
        Double longitude = Double.valueOf(talkParams.get(TALK_LONGITUDE));
        Double latitude = Double.valueOf(talkParams.get(TALK_LATITUDE));
        // create new talk
        Talk res = createTalk(title, text, longitude, latitude);
        try {
            if (storeTalkInDB(res)) {
                talkList.add(res);//todo remove list later
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return res;
    }

    private boolean storeTalkInDB(Talk res) throws SQLException {
        Connection conn = openNewConnection();
        PreparedStatement statement = conn.prepareStatement(INSERT_NEW_TALK_SQL);
        statement.setTimestamp(1, new java.sql.Timestamp(res.getCreationDate().getTime()));
        statement.setString(2, res.getTitle());
        statement.setString(3, res.getText());
        statement.setDouble(4, res.getLocation().getLongitude());
        statement.setDouble(5, res.getLocation().getLatitude());
        System.out.println(statement.toString());
        return statement.execute();
    }

    private Talk getTalkById(String talkId) throws SQLException {
        Connection conn = openNewConnection();
        PreparedStatement statement = conn.prepareStatement(SELECT_TALK_BY_ID);
        statement.setInt(1, Integer.valueOf(talkId));
        System.out.println(statement.toString());
        ResultSet result = statement.executeQuery();
        while (result.next()) {
            Talk res = createTalk(BigInteger.valueOf(result.getInt(1)),
                    result.getDate(2),
                    result.getString(3),
                    result.getString(4),
                    result.getDouble(5),
                    result.getDouble(6));
            res.setAnswerList(getAnswersForTalk(talkId));
            return res;
        }
        throw new RuntimeException("Talk with id = " + talkId + " not found.");
    }

    private TreeSet<Answer> getAnswersForTalk(String talkId) throws SQLException {
        Connection conn = openNewConnection();
        PreparedStatement statement = conn.prepareStatement(SELECT_ANSWERS_BY_TALK_ID);
        statement.setInt(1, Integer.valueOf(talkId));
        System.out.println(statement.toString());
        ResultSet result = statement.executeQuery();
        TreeSet<Answer> answers = new TreeSet<Answer>();
        while (result.next()) {
            Answer answer = createAnswer(result.getLong(ANSWER_ID),
                    result.getLong(ANSWER_ORDER_NUM),
                    result.getLong(ANSWER_TALK_ID),
                    result.getDate(ANSWER_DATE),
                    result.getString(ANSWER_MSG));
            answers.add(answer);
        }
        return answers;
    }

    private Answer createAnswer(long answerId, long orderNumber, long talkId, Date answerDate, String msg) {
        Answer answer = new Answer();
        answer.setId(BigInteger.valueOf(answerId));
        answer.setOrderNumber(orderNumber);
        answer.setTalkId(BigInteger.valueOf(talkId));
        answer.setAnswerDate(answerDate);
        answer.setMessage(msg);
        return answer;
    }

    private Talk getTalkByIdOld(String talkId) {
        BigInteger id = new BigInteger(talkId);
        for (Talk t : talkList) {
            if (id.equals(t.getId())) {
                return t;
            }
        }
        throw new RuntimeException(String.format("Talk with ID %s no found!", id));
    }

    private synchronized Talk addNewAnswerToTalk(InputStream inputStream) throws IOException, SQLException {
        Map<String, String> talkParams = getParamsFromInputStream(inputStream);
        // parse request params
        String talkId = talkParams.get(TALK_ID);
        String answerText = talkParams.get(ANSWER_TEXT);
        // add answer to talk
        return addNewAnswerToTalk(talkId, answerText);
    }

    private Talk addNewAnswer(Talk talk, String answerText) {
        Answer answer = new Answer();
        answer.setId(generateAnswerId());
        answer.setTalkId(talk.getId());
        answer.setAnswerDate(Calendar.getInstance().getTime());
        answer.setMessage(answerText);
        answer.setOrderNumber(getLastOrderNumber(talk));
        try {
            storeAnswerInDB(answer);
            talk.getAnswerList().add(answer);
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return talk;
    }

    private boolean storeAnswerInDB(Answer answer) throws SQLException, ClassNotFoundException {
        Connection conn = openNewConnection();
        PreparedStatement statement = conn.prepareStatement(INSERT_NEW_ANSWER_FOR_TALK_SQL);
        statement.setLong(1, answer.getTalkId().longValue());
        statement.setLong(2, answer.getOrderNumber());
        statement.setTimestamp(3, new Timestamp(answer.getAnswerDate().getTime()));
        statement.setString(4, answer.getMessage());
        System.out.println(statement.toString());
        return statement.execute();
    }

    private Talk createTalk(BigInteger id, Date creationDate, String title, String text, Double longitude, Double latitude) {
        Talk res = new Talk();
        res.setId(id);
        res.setCreationDate(creationDate);
        res.setTitle(title);
        res.setText(text);
        res.setLocation(new CustomLocation(longitude, latitude));
        return res;
    }

    private Talk createTalk(String title, String text, Double longitude, Double latitude) {
        Talk res = new Talk();
        res.setId(generateTalkId());
        res.setCreationDate(Calendar.getInstance().getTime());
        res.setTitle(title);
        res.setText(text);
        res.setLocation(new CustomLocation(longitude, latitude));
        return res;
    }

    private synchronized BigInteger generateTalkId() {
        if (talkIdSeq == null) {
            talkIdSeq = new BigInteger("0");
        } else {
            talkIdSeq = talkIdSeq.add(new BigInteger("1"));
        }
        return talkIdSeq;
    }

    private synchronized BigInteger generateAnswerId() {
        if (answerIdSeq == null) {
            answerIdSeq = new BigInteger("0");
        } else {
            answerIdSeq = answerIdSeq.add(new BigInteger("1"));
        }
        return answerIdSeq;
    }

    private long getLastOrderNumber(Talk talk) {
        synchronized (talk) {
            return talk.getAnswerList().isEmpty() ? 1 : talk.getAnswerList().last().getOrderNumber() + 1;
        }
    }

    private synchronized Talk addNewAnswerToTalk(String talkId, String answerText) throws SQLException {
        Talk talk = this.getTalkById(talkId);
        talk = addNewAnswer(talk, answerText);
        // DEBUG info
        System.out.println("Adding new answer to talk with ID: " + talkId);
        System.out.println("Talk has answers:");
        for (Answer answer : talk.getAnswerList()) {
            System.out.println(String.format("%d: %s", answer.getOrderNumber(), answer.getMessage()));
        }
        return talk;
    }

    private static Map<String, String> getParamsFromInputStream(InputStream inputStream) throws IOException {
        try {
            byte[] bytes = IOUtils.toByteArray(inputStream);
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
            ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);
            try {
                return (Map<String, String>) objectInputStream.readObject();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            return new HashMap<String, String>();
        } finally {
            inputStream.close();
        }
    }

    private static InputStream getInputStreamFromObject(Object object) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
        objectOutputStream.writeObject(object);
        objectOutputStream.flush();
        objectOutputStream.close();
        return new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
    }
}
