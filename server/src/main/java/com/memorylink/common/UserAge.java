package com.memorylink.common;

import com.memorylink.user.User;
import java.time.LocalDate;
import java.time.Period;

public final class UserAge {

    private UserAge() {
    }

    public static boolean isAdult(User user) {
        if (user == null || user.getBirthDate() == null) {
            return false;
        }
        return Period.between(user.getBirthDate(), LocalDate.now()).getYears() >= 18;
    }
}
