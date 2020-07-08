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
import java.util.Set;

import static com.telegrambot.funcompas.botapi.BotState.DELETE_FAVORITE;
@Component
public class DeleteFavoriteHandler implements InputMessageHandler{
    private final UserDataCache userDataCache;
    private final ReplyMessagesService messagesService;
    private final UsersProfileDataService profileDataService;
    private final PlaceMethodsService placeMethodsService;

    public DeleteFavoriteHandler(UserDataCache userDataCache, ReplyMessagesService messagesService, UsersProfileDataService profileDataService, PlaceMethodsService placeMethodsService) {
        this.userDataCache = userDataCache;
        this.messagesService = messagesService;
        this.profileDataService = profileDataService;
        this.placeMethodsService = placeMethodsService;
    }

   @Override
        public SendMessage handle(Message message) {
            if (userDataCache.getUsersCurrentBotState(message.getFrom().getId()).equals(DELETE_FAVORITE)) {
                userDataCache.setUsersCurrentBotState(message.getFrom().getId(), BotState.ASK_ID);
            }
            return processUsersInput(message);
        }

    private SendMessage processUsersInput(Message inputMsg) {
        String usersAnswer = inputMsg.getText();
        int userId = inputMsg.getFrom().getId();
        long chatId = inputMsg.getChatId();

        UserProfileData profileData = profileDataService.getUserProfileData(chatId);
        BotState botState = userDataCache.getUsersCurrentBotState(userId);

        SendMessage replyToUser = null;

        if (botState.equals(BotState.ASK_ID)) {
            replyToUser = messagesService.getReplyMessage(chatId, "reply.deleteFromFavorite");
            userDataCache.setUsersCurrentBotState(userId, BotState.DELETE_BUTTON);
        }

        if (botState.equals(BotState.DELETE_BUTTON)) {
            Set<Place> favorits = profileData.getFavorite();
            Optional<Place> place = placeMethodsService.findPlaceById(Integer.parseInt(usersAnswer));
            if (!place.isPresent()) {
                replyToUser = new SendMessage(chatId, "Элемент с таким id не найден!");
            } else {
                favorits.removeIf(p -> p.getId().equals(Integer.parseInt(usersAnswer)));
                profileData.setFavorite(favorits);
                replyToUser = new SendMessage(chatId, "Удалено!");
                profileDataService.save(profileData);
            }
            userDataCache.setUsersCurrentBotState(userId, BotState.DELETE_BUTTON);
            userDataCache.saveUserProfileData(userId, profileData);
        }  return replyToUser;
    }
    @Override
    public BotState getHandlerName() {
        return DELETE_FAVORITE;
    }

}
