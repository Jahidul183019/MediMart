package ui;

import models.User;

public class UserProfileForm {
    private User currentUser;

    public UserProfileForm(User user) {
        this.currentUser = user;
    }

    public void showUserInfo() {
        // Correct method call
        System.out.println("User ID: " + currentUser.getId());  // Using getId() instead of getUserId()
        System.out.println("User Email: " + currentUser.getEmail());
        System.out.println("User Name: " + currentUser.getFirstName() + " " + currentUser.getLastName());
        System.out.println("Phone: " + currentUser.getPhone());
    }
}
