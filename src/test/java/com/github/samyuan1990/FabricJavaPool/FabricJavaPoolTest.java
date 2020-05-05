/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package com.github.samyuan1990.FabricJavaPool;


import java.io.File;
import java.nio.file.Paths;
import java.util.Collection;
import static java.lang.String.format;
import com.github.samyuan1990.FabricJavaPool.util.SampleStore;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.hyperledger.fabric.sdk.*;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

public class FabricJavaPoolTest {

    private static String configUserPath = "./src/test/resources/crypto-config/peerOrganizations/org1.example.com/users/Admin@org1.example.com/msp/keystore";

    public static User getUser() {
        User appuser = null;
        File sampleStoreFile = new File(System.getProperty("user.home") + "/test.properties");
        if (sampleStoreFile.exists()) { //For testing start fresh
            sampleStoreFile.delete();
        }
        final SampleStore sampleStore = new SampleStore(sampleStoreFile);
        try {
            appuser = sampleStore.getMember("peer1", "Org1", "Org1MSP",
                    new File(String.valueOf(findFileSk(Paths.get(configUserPath).toFile()))),
                    new File("./src/test/resources/crypto-config/peerOrganizations/org1.example.com/users/Admin@org1.example.com/msp/signcerts/Admin@org1.example.com-cert.pem"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return appuser;
    }

    public static String query(Channel myChannel, String chainCodeName, String fcn, String... arguments) {
        String payload = "";
        try {
            ChaincodeID chaincodeID = ChaincodeID.newBuilder().setName(chainCodeName)
                    .setVersion("1.0")
                    .build();
            HFClient hfclient = HFClient.createNewInstance();
            QueryByChaincodeRequest transactionProposalRequest = hfclient.newQueryProposalRequest();
            transactionProposalRequest.setChaincodeID(chaincodeID);
            transactionProposalRequest.setFcn(fcn);
            transactionProposalRequest.setArgs(arguments);
            transactionProposalRequest.setUserContext(getUser());
            Collection<ProposalResponse> queryPropResp = myChannel.queryByChaincode(transactionProposalRequest);
            for (ProposalResponse response:queryPropResp) {
                if (response.getStatus() == ChaincodeResponse.Status.SUCCESS) {
                    payload = response.getProposalResponse().getResponse().getPayload().toStringUtf8();
                }
            }
        } catch (Exception e) {
            System.out.printf(e.toString());
        }
        return payload;
    }

    private static File findFileSk(File directory) {
        File[] matches = directory.listFiles((dir, name) -> name.endsWith("_sk"));
        if (null == matches) {
            throw new RuntimeException(format("Matches returned null does %s directory exist?", directory.getAbsoluteFile().getName()));
        }
        if (matches.length != 1) {
            throw new RuntimeException(format("Expected in %s only 1 sk file but found %d", directory.getAbsoluteFile().getName(), matches.length));
        }
        return matches[0];
    }

    @Test public void testChannelPool() {
        ObjectPool<Channel>  myChannelPool = new FabricJavaPool("./src/test/resources/Networkconfig.json", getUser(), "mychannel");
        try {
            Channel myChannel = myChannelPool.borrowObject();
            assertNotEquals("Test borrow item channel not null", myChannel, null);
            assertEquals("Test borrow item channel", myChannel.isInitialized(), true);
            Channel myChannel2 = myChannelPool.borrowObject();
            assertNotEquals("Test borrow item channel2 not null", myChannel2, null);
            assertEquals("Test borrow item channel2", myChannel2.isInitialized(), true);
            assertEquals("Test item should diff", myChannel2.equals(myChannel), false);
            System.out.println(System.getenv("ORG_GRADLE_PROJECT_LocalFabric"));
            if (System.getenv("ORG_GRADLE_PROJECT_LocalFabric").equals("true")) {
                String rs = query(myChannel, "mycc", "query", "a");
                assertEquals("90", rs);
                System.out.println(rs);
                String rs2 = query(myChannel2, "mycc", "query", "a");
                assertNotEquals("91", rs2);
            }
            myChannelPool.returnObject(myChannel);
            myChannelPool.returnObject(myChannel2);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test public void testChannelPoolException() {
        ObjectPool<Channel>  myChannelPool = new FabricJavaPool("./src/test/resources/Networkconfig.json", null, "mychannel");
        try {
            myChannelPool.borrowObject();
        } catch (Exception e) {
            assertNotNull(e);
        }
    }

    @Test public void testChannelPoolBorrow() {
        GenericObjectPoolConfig config = new GenericObjectPoolConfig();
        config.setMaxTotal(5);
        config.setMaxWaitMillis(1000);
        ObjectPool<Channel>  myChannelPool = new FabricJavaPool("./src/test/resources/Networkconfig.json", getUser(), "mychannel", config);
        try {
            for (int i = 0; i < 5; i++) {
                Channel o = myChannelPool.borrowObject();
                assertNotNull(o);
            }
        } catch (Exception e) {
            assertNull(e);
        }
        try {
            myChannelPool.borrowObject();
        } catch (Exception e) {
            assertNotNull(e);
        }
    }
}