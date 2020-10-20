package rafikibora.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rafikibora.exceptions.RafikiBoraException;
import rafikibora.exceptions.ResourceNotFoundException;
import rafikibora.model.account.Account;
import rafikibora.model.terminal.Terminal;
import rafikibora.model.transactions.Transaction;
import rafikibora.model.users.User;
import rafikibora.repository.AccountRepository;
import rafikibora.repository.TerminalRepository;
import rafikibora.repository.TransactionRepository;
import rafikibora.repository.UserRepository;

import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class includes functionality to send money to someone.
 * A unique token is generated and sent by email to the intended
 * recipient for every instance of this transaction.
 */
@Service
@Slf4j
public class SendMoneyService {

    private final String PROCESSING_CODE = "26000";
    /** Formats a date with such a format as: 04-09-2019 01:45:48 */
    private final String DATE_TIME_FORMAT_BIT_7 = "dd-MM-yyyy hh:mm:ss";

    @Autowired
    AccountRepository accountRepository;

    @Autowired
    TransactionRepository transactionRepository;

    @Autowired
    EmailService emailService;

    @Autowired
    TerminalRepository terminalRepository;

    @Autowired
    UserRepository userRepository;

    /**
     * This method is used to perform a send money transaction
     *
     * @param sendMoneyData
     */
    @Transactional
    public void sendMoney(Transaction sendMoneyData) {
        // Get required fields
        String merchantPan = sendMoneyData.getPan(); // 2
        String processingCode = sendMoneyData.getProcessingCode(); // 3
        Double amountToSend = sendMoneyData.getAmountTransaction(); //4
        String emailOfRecipient = sendMoneyData.getRecipientEmail(); // 47
        String currencyCode = sendMoneyData.getCurrencyCode(); // 49
        String dateTime = sendMoneyData.getDateTime(); // 7
        String TID = sendMoneyData.getTID(); // 41

        // Set a default value for the processing code if none provided
        processingCode = (processingCode == null ? PROCESSING_CODE: processingCode);

        SimpleDateFormat d = new SimpleDateFormat(DATE_TIME_FORMAT_BIT_7);
        try {
            Date date = d.parse(dateTime);
            dateTime = "" + date;
        } catch (Exception e) {

        }


        System.out.println("######### pan: " + merchantPan);
        System.out.println("######### process code: " + processingCode);
        System.out.println("######### amount: " + amountToSend);
        System.out.println("######### email: " + emailOfRecipient);
        System.out.println("######### currency code: " + currencyCode);
        System.out.println("######### datetime: " + dateTime);

        try {
            // Validate TID and MID
            validateTID(TID);

            // get merchant's account using pan
            Optional<Account> merchantAccount = accountRepository.findByPan(merchantPan);

            // Add amount to merchant's account
            merchantAccount.get().setBalance(merchantAccount.get().getBalance() + amountToSend);

            // Generate withdrawal token
            String recipientToken = generateRecipientToken(9); // Generates a unique ID or length 9

            // Add token to transaction model
            sendMoneyData.setToken(recipientToken);

            // Update Date-Time Field
            sendMoneyData.setDateTime(dateTime);

            // store transaction details
            transactionRepository.save(sendMoneyData);

            // send token to email of recipient

            emailService.sendEmail(emailOfRecipient, recipientToken);
        } catch (Exception ex) {
            log.error("Error sending money: " + ex.getMessage());
            throw new RafikiBoraException("Error sending money: " + ex.getMessage());
        }

    }

    /**
     * This method generates a unique token
     *
     * @param tokenLength Specifies the length of the token
     * @return UUID a unique token value
     */
    private String generateRecipientToken(int tokenLength) {
        return UUID.randomUUID().toString().substring(0, tokenLength);
    }

    /**
     * Ensures that the Terminal Identification is valid
     * @param TID Indentication Number
     */
    private void validateTID(String TID) {
        Optional<Terminal> terminal = terminalRepository.findByTid(TID);
        if (!terminal.isPresent()) {
            throw new ResourceNotFoundException("Invalid Terminal Identification Number");
        }
    }

    /**
     * Checks that the Merchant Identification number is valid
     *
     * @param MID Merchant Identification Number
     */
    private void validateMID(String MID) {
        Optional<User> merchant = userRepository.findByMid(MID);
        if (merchant == null) {
            throw new ResourceNotFoundException("Invalid Merchant Identification Number");
        }
    }


    /**
     * Formats a string representation of date and time
     * into a valid format that can be parsed by the DateFormatter object.
     *
     * @param dateTimeString a string formatted as 'yymmddhhmmss'
     * @return a string with the format 'dd-MM-yyyy hh:mm:ss'
     */
    private String parseDateTime(String dateTimeString) {
        Pattern dayTime = Pattern.compile("(\\d\\d)(\\d\\d)(\\d\\d)(\\d\\d)(\\d\\d)(\\d\\d)");
        Matcher m = dayTime.matcher(dateTimeString);

        String returnVal = null;
        if (m.find()) {
            String year =  m.group(1);
            String month =  m.group(2);
            String day =  m.group(3);
            String hr =  m.group(4);
            String min =  m.group(5);
            String sec =  m.group(6);
            returnVal = day + "-" + month + "-" + "20" + year + " " + hr + ":" + min + ":" + sec;
        }
        return returnVal;
    }

    public static void main(String[] args) {
        SendMoneyService s = new SendMoneyService();
        System.out.println(s.parseDateTime("201020000000"));
        String formattedDateTimeString = s.parseDateTime("201020000000");
    }
}