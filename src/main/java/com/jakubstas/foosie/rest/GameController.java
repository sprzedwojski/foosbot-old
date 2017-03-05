package com.jakubstas.foosie.rest;

import com.jakubstas.foosie.configuration.SlackProperties;
import com.jakubstas.foosie.service.GameService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController("game")
public class GameController {

    private static final String LIST = "list";
    private static final String CANCEL = "cancel";
    private static final String UPDATE = "update";
    private static final String JOIN = "join";
    private static final String NEW = "new";
    private final Logger logger = LoggerFactory.getLogger(GameController.class);

    @Autowired
    private GameService gameService;

    @Autowired
    private SlackProperties slackProperties;

    @RequestMapping(method = RequestMethod.POST, consumes = "application/x-www-form-urlencoded;charset=UTF-8")
    public void doAction(@RequestParam(value = "response_url") String responseUrl,
                           @RequestParam(value = "token") String token,
                           @RequestParam(value = "user_name") String userName,
                           @RequestParam(value = "user_id") String userId,
                           @RequestParam(value = "text", required = false) String text) {

        if (!slackProperties.getNewCommandToken().equals(token)) {
            String msg = "Cannot perform action - invalid token!";
            logger.warn(msg);
            gameService.reply(responseUrl, msg);
            return;
        }

        String action = "", args = "";
        if(text != null) {
            String[] arr = text.split(" ");
            action = arr[0];
            logger.info("action: " + action);
            args = Arrays.stream(arr).skip(1).reduce("", (a, b) -> a + b + " ");
            logger.info("args  : " + args);
        }

        if(NEW.equals(action)) {
            createGame(userName, userId, args.trim(), responseUrl);
        } else if(JOIN.equals(action)) {
            joinGame(userName, userId, args.trim(), responseUrl);
        } else if(UPDATE.equals(action)) {
            updateGame(userName, args.trim(), responseUrl);
        } else if(CANCEL.equals(action)) {
            cancelGame(userName, responseUrl);
        } else if(LIST.equals(action)) {
            getStatus(responseUrl);
        } else {
            gameService.reply(responseUrl, "Sorry, no action was matched to your command. Try one of the following: "
                    + "[new HH:MM | join | update | cancel | list]");
        }
    }

    private void createGame(String userName, String userId, String text, String responseUrl) {
        gameService.createGame(userName, userId, responseUrl, text);
    }

    private void joinGame(String userName, String userId, String hostName, String responseUrl) {
        final Optional<String> hostNameOptional = StringUtils.hasText(hostName) ?
                Optional.of(hostName) : Optional.empty();
        gameService.joinGame(userName, userId, hostNameOptional, responseUrl);
    }

    private void updateGame(String userName, String proposedTime, String responseUrl) {
        gameService.updateGame(userName, responseUrl, proposedTime);
    }

    private void cancelGame(String userName, String responseUrl) {
        gameService.cancelGame(userName, responseUrl);
    }

    private void getStatus(String responseUrl) {
        gameService.getStatus(responseUrl);
    }
}
