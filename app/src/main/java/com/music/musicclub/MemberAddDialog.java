package com.music.musicclub;

import android.content.Context;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public class MemberAddDialog {
    public interface Callback { void onUserId(int userId); }

    // Backwards-compatible entry: no exclude list
    public static void show(@NonNull Context ctx, @NonNull Callback cb) {
        ClassDetailFragment.MemberPickerDialog.show(ctx, new ArrayList<>(), (userId, roleId, roleName) -> cb.onUserId(userId));
    }

    // New API: allow callers to pass exclude list
    public static void show(@NonNull Context ctx, @NonNull List<Integer> excludeIds, @NonNull Callback cb) {
        ClassDetailFragment.MemberPickerDialog.show(ctx, excludeIds, (userId, roleId, roleName) -> cb.onUserId(userId));
    }
}
