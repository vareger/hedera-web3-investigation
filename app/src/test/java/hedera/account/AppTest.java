/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package hedera.account;

import com.hedera.hashgraph.sdk.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.*;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.DefaultBlockParameterNumber;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.Transfer;
import org.web3j.utils.Convert;
import org.web3j.utils.Numeric;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

class AppTest {

    /// Testnet chain id
    private static final long CHAIN_ID = 0x128L;
    /// ECDSA private key created on Testnet
    private static final PrivateKey PRIVATE_KEY = PrivateKey.fromStringECDSA("e3ad95aa7e9678e96fb3d867c789e765db97f9d2018fca4068979df0832a5178");
    private static final PrivateKey PRIVATE_KEY_ED = PrivateKey.fromString("302e020100300506032b657004220420fcfcaa557eddf3b5b6fd8f28b64c00a456adcb0ecd4c44680f5b21b116758445");
    private static final AccountId ACCOUNT = AccountId.fromString("0.0.47848005");
    private static final AccountId ACCOUNT_ED = AccountId.fromString("0.0.47623543");
    private static final Credentials CREDENTIALS = Credentials.create(PRIVATE_KEY.toStringRaw());

    Web3j web3j;
    Client eddsaClient;
    Client ecdsaClient;

    @BeforeEach
    void before() {
//        web3j = Web3j.build(new HttpService("https://testnet.hashio.io/api"));
        web3j = Web3j.build(new HttpService("http://localhost:7546"));
        eddsaClient = createEDDSAClient();
        ecdsaClient = createECDSAClient();
    }

    @Test
    void appHasAGreeting() {
        App classUnderTest = new App();
        assertNotNull(classUnderTest.getGreeting(), "app should have a greeting");
    }

    @Test
    void web3Transfer() throws Exception {
        var gasPrice = web3j.ethGasPrice().send().getGasPrice();
        var transfer = new Transfer(web3j, new RawTransactionManager(web3j, CREDENTIALS, CHAIN_ID));

        var receipt = transfer.sendFunds(CREDENTIALS.getAddress(), BigDecimal.ONE, Convert.Unit.ETHER, gasPrice, Transfer.GAS_LIMIT).send();

        System.out.printf(receipt.toString());

        assertTrue(receipt.isStatusOK());
    }

    @Test
    void web3RawTransfer() throws Exception {
        var nonce = web3j.ethGetTransactionCount(ACCOUNT.toString(), DefaultBlockParameterName.LATEST).send();
        var tx = RawTransaction.createEtherTransaction(
                nonce.getTransactionCount(),
                Convert.toWei(BigDecimal.valueOf(2000), Convert.Unit.GWEI).toBigInteger(),
                Transfer.GAS_LIMIT.multiply(BigInteger.TEN),
                CREDENTIALS.getAddress(),
                Convert.toWei(BigDecimal.valueOf(1), Convert.Unit.ETHER).toBigInteger()
        );

        var signed = TransactionEncoder.signMessage(tx, CHAIN_ID, CREDENTIALS);

        var sent = web3j.ethSendRawTransaction(Numeric.toHexString(signed)).send();

        if (sent.hasError()) {
            System.out.println(sent.getError().getMessage());
            System.out.println(sent.getError().getData());
            fail();
        }

        var receipt = web3j.ethGetTransactionReceipt(sent.getTransactionHash()).send();

        if (receipt.hasError()) {
            System.out.println(receipt.getError().getMessage());
            System.out.println(receipt.getError().getData());
            fail();
        }

        var transactionReceipt = receipt.getTransactionReceipt().get();

        assertTrue(transactionReceipt.isStatusOK());
    }

    @Test
    void hashTransferEC() throws PrecheckStatusException, TimeoutException, ReceiptStatusException {
        var transfer = new TransferTransaction()
                .addHbarTransfer(ecdsaClient.getOperatorAccountId(), Hbar.from(-1))
                .addHbarTransfer(ACCOUNT_ED, Hbar.from(1))
                .execute(ecdsaClient);

        System.out.println(transfer.toString());
        assertEquals(Status.SUCCESS, transfer.getReceipt(ecdsaClient).status);
    }

    @Test
    void callUNNContract() throws IOException {
        var usdcAddress = "0x0000000000000000000000000000000002da3b48";
        var usdcId = "0.0.47856456";
        var balanceOf = new Function("balanceOf", Collections.singletonList(new Address("0x0000000000000000000000000000000002da1a45")), Collections.singletonList(new TypeReference<Uint256>() {}));

        var encoded = FunctionEncoder.encode(balanceOf);

        var result = web3j.ethCall(Transaction.createEthCallTransaction(null, usdcAddress, encoded), DefaultBlockParameterName.LATEST).send();

        if (result.hasError()) {
            System.out.println(result.getError().getMessage());
            System.out.println(result.getError().getData());
            fail();
        }

        System.out.println(result.getValue());
    }

    @Test
    void executeUNNContract() throws IOException {
        var usdcAddress = "0x0000000000000000000000000000000002da3b48";
        var usdcId = "0.0.47856456";
        var nonce = web3j.ethGetTransactionCount(CREDENTIALS.getAddress(), DefaultBlockParameterName.LATEST).send();
        var approve = new Function("approve", Arrays.asList(new Address(CREDENTIALS.getAddress()), new Uint256(100)), Collections.singletonList(new TypeReference<Bool>() {}));

        var encoded = FunctionEncoder.encode(approve);

        var tx = RawTransaction.createTransaction(
                nonce.getTransactionCount(),
                Convert.toWei(BigDecimal.valueOf(2000), Convert.Unit.GWEI).toBigInteger(),
                Transfer.GAS_LIMIT.multiply(BigInteger.TEN),
                usdcAddress,
                encoded
        );

        var signed = TransactionEncoder.signMessage(tx, CHAIN_ID, CREDENTIALS);

        var sent = web3j.ethSendRawTransaction(Numeric.toHexString(signed)).send();

        if (sent.hasError()) {
            System.out.println(sent.getError().getMessage());
            System.out.println(sent.getError().getData());
            fail();
        }

        System.out.println(sent.getTransactionHash());

        var receipt = web3j.ethGetTransactionReceipt(sent.getTransactionHash()).send();

        if (receipt.hasError()) {
            System.out.println(receipt.getError().getMessage());
            System.out.println(receipt.getError().getData());
            fail();
        }

        var transactionReceipt = receipt.getTransactionReceipt().get();

        assertTrue(transactionReceipt.isStatusOK());
    }

    @Test
    void getTransferLogs() throws IOException {
        var usdcAddress = "0x0000000000000000000000000000000002da3b48";
        var usdcId = "0.0.47856456";
        var transfer = new Event("Transfer", Arrays.asList(
                new TypeReference<Address>(true) {},
                new TypeReference<Address>(true) {},
                new TypeReference<Uint256>(false) {}
        ));

        var topic0 = EventEncoder.encode(transfer);

        var blockNumber = web3j.ethBlockNumber().send().getBlockNumber();

        var filter = new EthFilter(new DefaultBlockParameterNumber(BigInteger.ZERO), new DefaultBlockParameterNumber(blockNumber), usdcAddress);
        filter.addSingleTopic(topic0);

        var result = web3j.ethGetLogs(filter).send();

        if (result.hasError()) {
            System.out.println(result.getError().getMessage());
            System.out.println(result.getError().getData());
            fail();
        }

        System.out.println(result.getLogs());
    }

    @Test
    void getBalance() throws IOException {
        var result = web3j.ethGetBalance(ACCOUNT.toString(), DefaultBlockParameterName.LATEST).send();

        if (result.hasError()) {
            System.out.println(result.getError().getMessage());
            System.out.println(result.getError().getData());
            fail();
        }

        System.out.println(result.getBalance());
    }

    @Test
    void getNonce() throws IOException {
        var result = web3j.ethGetTransactionCount(CREDENTIALS.getAddress(), DefaultBlockParameterName.LATEST).send();

        if (result.hasError()) {
            System.out.println(result.getError().getMessage());
            System.out.println(result.getError().getData());
            fail();
        }

        System.out.println(result.getTransactionCount());
    }

    @Test
    void getTransactionReceipt() throws IOException {
        {
            var id = "0.0.46847518-1659965320-401607201";

            var receipt = web3j.ethGetTransactionReceipt(id).send();

            if (receipt.hasError()) {
                System.out.println(receipt.getError().getMessage());
                System.out.println(receipt.getError().getData());
                fail();
            }

            var transactionReceipt = receipt.getTransactionReceipt().get();

            assertTrue(transactionReceipt.isStatusOK());
        }
    }

    @Test
    void getTransaction() throws IOException {
        var id = "0.0.46847518-1659965320-401607201";

        var receipt = web3j.ethGetTransactionByHash(id).send();

        if (receipt.hasError()) {
            System.out.println(receipt.getError().getMessage());
            System.out.println(receipt.getError().getData());
            fail();
        }

        var transactionReceipt = receipt.getTransaction().get();

        System.out.println(transactionReceipt.getInput());
    }

    private Client createEDDSAClient() {
        //Create your Hedera testnet client
        Client client = Client.forTestnet();
        client.setOperator(ACCOUNT_ED, PRIVATE_KEY_ED);

        return client;
    }

    private Client createECDSAClient() {
        //Create your Hedera testnet client
        Client client = Client.forTestnet();
        client.setOperator(ACCOUNT, PRIVATE_KEY);

        return client;
    }
}