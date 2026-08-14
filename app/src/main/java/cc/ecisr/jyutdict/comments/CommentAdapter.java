package cc.ecisr.jyutdict.comments;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.text.HtmlCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

import cc.ecisr.jyutdict.R;
import cc.ecisr.jyutdict.auth.AuthRepository;

final class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.ViewHolder> {
    private final ArrayList<Comment> comments = new ArrayList<>();
    private final DeleteListener deleteListener;
    private AuthRepository.User currentUser;
    private boolean isAdmin;

    CommentAdapter(DeleteListener deleteListener) {
        this.deleteListener = deleteListener;
        setHasStableIds(true);
    }

    void submit(List<Comment> values, AuthRepository.User user, boolean admin) {
        ArrayList<Comment> previous = new ArrayList<>(comments);
        AuthRepository.User previousUser = currentUser;
        boolean previousAdmin = isAdmin;
        ArrayList<Comment> updated = new ArrayList<>(values);
        DiffUtil.DiffResult diff = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return previous.size();
            }

            @Override
            public int getNewListSize() {
                return updated.size();
            }

            @Override
            public boolean areItemsTheSame(int oldPosition, int newPosition) {
                return previous.get(oldPosition).id == updated.get(newPosition).id;
            }

            @Override
            public boolean areContentsTheSame(int oldPosition, int newPosition) {
                Comment oldComment = previous.get(oldPosition);
                Comment newComment = updated.get(newPosition);
                return sameContent(oldComment, newComment)
                        && canDelete(oldComment, previousUser, previousAdmin)
                        == canDelete(newComment, user, admin);
            }
        });
        comments.clear();
        comments.addAll(updated);
        currentUser = user;
        isAdmin = admin;
        diff.dispatchUpdatesTo(this);
    }

    private static boolean sameContent(Comment left, Comment right) {
        return left.userId == right.userId
                && left.deleted == right.deleted
                && left.content.equals(right.content)
                && left.createdAt.equals(right.createdAt)
                && left.updatedAt.equals(right.updatedAt)
                && left.nickname.equals(right.nickname)
                && left.email.equals(right.email)
                && left.role.equals(right.role);
    }

    private static boolean canDelete(Comment comment, AuthRepository.User user, boolean admin) {
        return !comment.deleted && (admin || user != null && comment.userId == user.id);
    }

    int activeCount() {
        int count = 0;
        for (Comment comment : comments) if (!comment.deleted) count++;
        return count;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.layout_comment_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Comment comment = comments.get(position);
        if (comment.deleted) {
            holder.author.setText(R.string.comment_deleted);
            holder.meta.setText("");
            holder.content.setText("");
            holder.content.setVisibility(View.GONE);
            holder.delete.setVisibility(View.GONE);
            return;
        }

        holder.author.setText(comment.displayName());
        holder.meta.setText(holder.itemView.getContext().getString(
                R.string.comment_meta, comment.role, comment.createdAt));
        holder.content.setText(HtmlCompat.fromHtml(
                comment.content.replace("\n", "<br>"),
                HtmlCompat.FROM_HTML_MODE_LEGACY));
        holder.content.setVisibility(View.VISIBLE);
        boolean ownComment = currentUser != null && comment.userId == currentUser.id;
        holder.delete.setVisibility(ownComment || isAdmin ? View.VISIBLE : View.GONE);
        holder.delete.setOnClickListener(view -> deleteListener.onDelete(comment));
    }

    @Override
    public int getItemCount() {
        return comments.size();
    }

    @Override
    public long getItemId(int position) {
        return comments.get(position).id;
    }

    static final class ViewHolder extends RecyclerView.ViewHolder {
        final TextView author;
        final TextView meta;
        final TextView content;
        final MaterialButton delete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            author = itemView.findViewById(R.id.comment_author);
            meta = itemView.findViewById(R.id.comment_meta);
            content = itemView.findViewById(R.id.comment_content);
            delete = itemView.findViewById(R.id.comment_delete);
        }
    }

    interface DeleteListener {
        void onDelete(Comment comment);
    }
}
