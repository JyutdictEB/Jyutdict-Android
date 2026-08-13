package cc.ecisr.jyutdict.comments;

import org.json.JSONObject;

/** A comment returned by either Jyutdict comment endpoint. */
public final class Comment {
    public final int id;
    public final int userId;
    public final String content;
    public final boolean deleted;
    public final String createdAt;
    public final String updatedAt;
    public final String nickname;
    public final String email;
    public final String role;

    private Comment(
            int id,
            int userId,
            String content,
            boolean deleted,
            String createdAt,
            String updatedAt,
            String nickname,
            String email,
            String role
    ) {
        this.id = id;
        this.userId = userId;
        this.content = content;
        this.deleted = deleted;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.nickname = nickname;
        this.email = email;
        this.role = role;
    }

    static Comment fromJson(JSONObject json) {
        return new Comment(
                json.optInt("id"),
                json.optInt("user_id"),
                json.isNull("content") ? "" : json.optString("content", ""),
                json.optInt("is_deleted", 0) != 0,
                json.optString("created_at", ""),
                json.optString("updated_at", ""),
                json.optString("nickname", ""),
                json.optString("email", ""),
                json.optString("role", "user")
        );
    }

    public String displayName() {
        if (!nickname.isEmpty()) return nickname;
        int at = email.indexOf('@');
        return at > 0 ? email.substring(0, at) : email;
    }
}
