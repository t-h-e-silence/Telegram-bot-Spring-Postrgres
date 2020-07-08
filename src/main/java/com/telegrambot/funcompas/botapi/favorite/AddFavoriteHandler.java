package com.telegrambot.funcompas.botapi.favorite;

import com.telegrambot.funcompas.botapi.BotState;
import com.telegrambot.funcompas.botapi.handlers.InputMessageHandler;
import com.telegrambot.funcompas.cache.UserDataCache;
import com.telegrambot.funcompas.entity.Place;
import com.telegrambot.funcompas.entity.UserProfileData;
import com.telegrambot.funcompas.service.PlaceMethodsService;
import com.telegrambot.funcompas.service.ReplyMessagesService;
import com.telegrambot.funcompas.service.UsersProfileDataService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import java.util.Optional;


@Component
public class AddFavoriteHandler implements InputMessageHandler  {

    private final UserDataCache userDataCache;
    private final ReplyMessagesService messagesService;
    private final PlaceMethodsService placeMethodsService;
    private final UsersProfileDataService profileDataService;

    public AddFavoriteHandler(UserDataCache userDataCache, ReplyMessagesService messagesService,
                              PlaceMethodsService placeMethodsService, UsersProfileDataService profileDataService) {
        this.userDataCache = userDataCache;
        this.messagesService = messagesService;
        this.placeMethodsService = placeMethodsService;
        this.profileDataService = profileDataService;
    }

    @Override
    public SendMessage handle(Message message) {
        if (userDataCache.getUsersCurrentBotState(message.getFrom().getId()).equals(BotState.ADD_TO_FAVORITE)) {
            userDataCache.setUsersCurrentBotState(message.getFrom().getId(), BotState.ASK_ID);
        }
        return processUsersInput(message);
    }

    @Override
    public BotState getHandlerName() {
        return BotState.ADD_TO_FAVORITE;
    }

    private SendMessage processUsersInput(Message inputMsg) {
        String usersAnswer = inputMsg.getText();
        int userId = inputMsg.getFrom().getId();
        long chatId = inputMsg.getChatId();

        UserProfileData profileData = userDataCache.getUserProfileData(userId);
        BotState botState = userDataCache.getUsersCurrentBotState(userId);

        SendMessage replyToUser = null;

        if (botState.equals(BotState.ASK_ID)) {
            replyToUser = messagesService.getReplyMessage(chatId, "reply.addToFavorite");
            userDataCache.setUsersCurrentBotState(userId, BotState.PROFILE_FILLED);
        }

        if (botState.equals(BotState.PROFILE_FILLED)) {
            profileData.setChatId(chatId);
            Optional<Place> favorite = placeMethodsService.findPlaceById(Integer.parseInt(usersAnswer));
            if (!favorite.isPresent()) {
                replyToUser = new SendMessage(chatId, "Элемент с таким id не найден!");
            } else {
                profileData.addFavorite(favorite);
                profileDataService.save(profileData);

                userDataCache.setUsersCurrentBotState(userId, BotState.MAIN_MENU);

                String profileFilledMessage = messagesService.getReplyText("reply.profileFilled",
                        profileData.getFavorite().toString());
                replyToUser = new SendMessage(chatId, profileFilledMessage);
            }
        }
        usersAnswer=null;
        userDataCache.saveUserProfileData(userId, profileData);
        return replyToUser;
    }
}



