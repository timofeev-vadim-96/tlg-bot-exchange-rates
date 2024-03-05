package org.example.controller.users;

import org.example.dao.Dao;
import org.example.model.User.Feedback;
import org.example.model.User.Subscription;
import org.example.model.User.UserApp;
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
        UserApp userApp = new UserApp();

        controller.add(userApp);

        verify(dao, times(1)).save(userApp);
    }

    @Test
    void removeById() {
        UserApp userApp = new UserApp.Builder().withId(1).build();

        controller.remove(userApp.getId());

        verify(dao, times(1)).delete(UserApp.class, userApp.getId());
    }

    @Test
    void removeEntity() {
        UserApp userApp = new UserApp();

        controller.remove(userApp);

        verify(dao, times(1)).delete(userApp);
    }

    @Test
    void get() {
        UserApp userApp = new UserApp.Builder().withId(1).build();
        when(dao.get(1, UserApp.class)).thenReturn(userApp);

        UserApp returnedUserApp = controller.get(1);

        assertEquals(userApp, returnedUserApp);
    }

    @Test
    void isContainedPositive() {
        UserApp userApp = new UserApp.Builder().withId(1).build();
        when(dao.findAll(UserApp.class)).thenReturn(List.of(userApp));

        assertTrue(controller.isContained(userApp.getId()));
    }

    @Test
    void isContainedNegative() {
        UserApp userApp = new UserApp.Builder().withId(2).build();
        when(dao.findAll(UserApp.class)).thenReturn(List.of(userApp));

        assertFalse(controller.isContained(1));
    }

    @Test
    void isSignedPositive() {
        UserApp userApp = new UserApp.Builder().withId(1).build();
        String charCode = "USD";
        userApp.addSubscription(charCode);
        when(dao.get(userApp.getId(), UserApp.class)).thenReturn(userApp);

        assertTrue(controller.isSigned(userApp.getId(), charCode));
    }

    @Test
    void isSignedNegative() {
        UserApp userApp = new UserApp.Builder().withId(1).build();
        userApp.addSubscription("USD");
        when(dao.get(userApp.getId(), UserApp.class)).thenReturn(userApp);

        assertFalse(controller.isSigned(userApp.getId(), "EUR"));
    }

    @Test
    void addSubscriptionWhenDbAlreadyContainsValute() {
        UserApp userApp = new UserApp.Builder().withId(1).build();
        when(dao.get(userApp.getId(), UserApp.class)).thenReturn(userApp);
        Subscription subscription = new Subscription("USD");
        when(dao.findAll(Subscription.class)).thenReturn(List.of(subscription));

        controller.addSubscription(userApp.getId(), "USD");

        verify(dao, times(1)).get(userApp.getId(), UserApp.class);
        verify(dao, times(1)).findAll(Subscription.class);
        verify(dao, never()).save(subscription);
        verify(dao, times(1)).update(userApp);
    }
    @Test
    void addSubscriptionWhenDbIsNotContainsValute() {
        UserApp userApp = new UserApp.Builder().withId(1).build();
        when(dao.get(userApp.getId(), UserApp.class)).thenReturn(userApp);
        Subscription subscription = new Subscription("USD");
        when(dao.findAll(Subscription.class)).thenReturn(List.of(subscription));

        controller.addSubscription(userApp.getId(), "EUR");

        verify(dao, times(1)).get(userApp.getId(), UserApp.class);
        verify(dao, times(1)).findAll(Subscription.class);
        verify(dao, times(1)).save(any(Subscription.class));
        verify(dao, times(1)).update(userApp);
    }

    @Test
    void removeSubscription() {
        UserApp userApp = new UserApp.Builder().withId(1).build();
        userApp.addSubscription("USD");
        when(dao.get(userApp.getId(), UserApp.class)).thenReturn(userApp);

        controller.removeSubscription(userApp.getId(), "USD");

        assertFalse(userApp.getSubscriptions().stream()
                .map(Subscription::getCharCode)
                .collect(Collectors.toSet())
                .contains("USD"));
        verify(dao, times(1)).get(userApp.getId(), UserApp.class);
        verify(dao, times(1)).update(userApp);
    }

    @Test
    void addFeedback() {
        UserApp userApp = new UserApp.Builder().withId(1).build();
        when(dao.get(userApp.getId(), UserApp.class)).thenReturn(userApp);

        String feedbackText = "Some text";
        controller.addFeedback(userApp.getId(), feedbackText);
        boolean isContainedNewFeedback = false;
        for (Feedback feedback: userApp.getFeedbackMessages()){
            if (feedback.getText().contains(feedbackText)) isContainedNewFeedback = true;
        }

        assertTrue(isContainedNewFeedback);
        verify(dao, times(1)).get(userApp.getId(), UserApp.class);
        verify(dao, times(1)).update(userApp);
    }

    @Test
    void removeFeedback() {
        UserApp userApp = new UserApp.Builder().withId(1).build();
        when(dao.get(userApp.getId(), UserApp.class)).thenReturn(userApp);
        Feedback feedback = new Feedback("Some text");
        feedback.setId(5);
        userApp.addFeedback(feedback);

        controller.removeFeedback(userApp.getId(), feedback.getId());

        assertFalse(userApp.getFeedbackMessages().contains(feedback));
        verify(dao, times(1)).get(userApp.getId(), UserApp.class);
        verify(dao, times(1)).update(userApp);
    }

    @Test
    void getUsers() {
        controller.getUsers();

        verify(dao, times(1)).findAll(UserApp.class);
    }
}