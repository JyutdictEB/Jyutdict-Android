package cc.ecisr.jyutdict.comments;

import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
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
    }

    void submit(List<Comment> values, AuthRepository.User user, boolean admin) {
        comments.clear();
        comments.addAll(values);
        currentUser = user;
        isAdmin = admin;
        notifyDataSetChanged();
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
        holder.content.setText(Html.fromHtml(
                comment.content.replace("\n", "<br>"), Html.FROM_HTML_MODE_LEGACY));
        holder.content.setVisibility(View.VISIBLE);
        boolean ownComment = currentUser != null && comment.userId == currentUser.id;
        holder.delete.setVisibility(ownComment || isAdmin ? View.VISIBLE : View.GONE);
        holder.delete.setOnClickListener(view -> deleteListener.onDelete(comment));
    }

    @Override
    public int getItemCount() {
        return comments.size();
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
