package org.example.controller.users;

import org.example.dao.Dao;
import org.example.model.User.CustomUser;
import org.example.model.User.Feedback;
import org.example.model.User.Subscription;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UsersControllerTest {
    private static Dao dao;
    private static UsersController controller;

    @BeforeAll
    static void setUp() {
        dao = mock(Dao.class);
        controller = new UsersController(dao);
    }

    @BeforeEach
    void resetMock(){
        reset(dao);
    }

    @Test
    void add() {
        CustomUser customUser = new CustomUser();

        controller.add(customUser);

        verify(dao, times(1)).save(customUser);
    }

    @Test
    void removeById() {
        CustomUser customUser = new CustomUser.Builder().withId(1).build();

        controller.remove(customUser.getId());

        verify(dao, times(1)).delete(CustomUser.class, customUser.getId());
    }

    @Test
    void removeEntity() {
        CustomUser customUser = new CustomUser();

        controller.remove(customUser);

        verify(dao, times(1)).delete(customUser);
    }

    @Test
    void get() {
        CustomUser customUser = new CustomUser.Builder().withId(1).build();
        when(dao.get(1, CustomUser.class)).thenReturn(customUser);

        CustomUser returnedCustomUser = controller.get(1);

        assertEquals(customUser, returnedCustomUser);
    }

    @Test
    void isContainedPositive() {
        CustomUser customUser = new CustomUser.Builder().withId(1).build();
        when(dao.findAll(CustomUser.class)).thenReturn(List.of(customUser));

        assertTrue(controller.isContained(customUser.getId()));
    }

    @Test
    void isContainedNegative() {
        CustomUser customUser = new CustomUser.Builder().withId(2).build();
        when(dao.findAll(CustomUser.class)).thenReturn(List.of(customUser));

        assertFalse(controller.isContained(1));
    }

    @Test
    void isSignedPositive() {
        CustomUser customUser = new CustomUser.Builder().withId(1).build();
        String charCode = "USD";
        customUser.addSubscription(charCode);
        when(dao.get(customUser.getId(), CustomUser.class)).thenReturn(customUser);

        assertTrue(controller.isSigned(customUser.getId(), charCode));
    }

    @Test
    void isSignedNegative() {
        CustomUser customUser = new CustomUser.Builder().withId(1).build();
        customUser.addSubscription("USD");
        when(dao.get(customUser.getId(), CustomUser.class)).thenReturn(customUser);

        assertFalse(controller.isSigned(customUser.getId(), "EUR"));
    }

    @Test
    void addSubscriptionWhenDbAlreadyContainsValute() {
        CustomUser customUser = new CustomUser.Builder().withId(1).build();
        when(dao.get(customUser.getId(), CustomUser.class)).thenReturn(customUser);
        Subscription subscription = new Subscription("USD");
        when(dao.findAll(Subscription.class)).thenReturn(List.of(subscription));

        controller.addSubscription(customUser.getId(), "USD");

        verify(dao, times(1)).get(customUser.getId(), CustomUser.class);
        verify(dao, times(1)).findAll(Subscription.class);
        verify(dao, never()).save(subscription);
        verify(dao, times(1)).update(customUser);
    }
    @Test
    void addSubscriptionWhenDbIsNotContainsValute() {
        CustomUser customUser = new CustomUser.Builder().withId(1).build();
        when(dao.get(customUser.getId(), CustomUser.class)).thenReturn(customUser);
        Subscription subscription = new Subscription("USD");
        when(dao.findAll(Subscription.class)).thenReturn(List.of(subscription));

        controller.addSubscription(customUser.getId(), "EUR");

        verify(dao, times(1)).get(customUser.getId(), CustomUser.class);
        verify(dao, times(1)).findAll(Subscription.class);
        verify(dao, times(1)).save(any(Subscription.class));
        verify(dao, times(1)).update(customUser);
    }

    @Test
    void removeSubscription() {
        CustomUser customUser = new CustomUser.Builder().withId(1).build();
        customUser.addSubscription("USD");
        when(dao.get(customUser.getId(), CustomUser.class)).thenReturn(customUser);

        controller.removeSubscription(customUser.getId(), "USD");

        assertFalse(customUser.getSubscriptions().stream()
                .map(Subscription::getCharCode)
                .collect(Collectors.toSet())
                .contains("USD"));
        verify(dao, times(1)).get(customUser.getId(), CustomUser.class);
        verify(dao, times(1)).update(customUser);
    }

    @Test
    void addFeedback() {
        CustomUser customUser = new CustomUser.Builder().withId(1).build();
        when(dao.get(customUser.getId(), CustomUser.class)).thenReturn(customUser);

        String feedbackText = "Some text";
        controller.addFeedback(customUser.getId(), feedbackText);
        boolean isContainedNewFeedback = false;
        for (Feedback feedback: customUser.getFeedbackMessages()){
            if (feedback.getText().contains(feedbackText)) isContainedNewFeedback = true;
        }

        assertTrue(isContainedNewFeedback);
        verify(dao, times(1)).get(customUser.getId(), CustomUser.class);
        verify(dao, times(1)).update(customUser);
    }

    @Test
    void removeFeedback() {
        CustomUser customUser = new CustomUser.Builder().withId(1).build();
        when(dao.get(customUser.getId(), CustomUser.class)).thenReturn(customUser);
        Feedback feedback = new Feedback("Some text");
        feedback.setId(5);
        customUser.addFeedback(feedback);

        controller.removeFeedback(customUser.getId(), feedback.getId());

        assertFalse(customUser.getFeedbackMessages().contains(feedback));
        verify(dao, times(1)).get(customUser.getId(), CustomUser.class);
        verify(dao, times(1)).update(customUser);
    }

    @Test
    void getUsers() {
        controller.getUsers();

        verify(dao, times(1)).findAll(CustomUser.class);
    }
}