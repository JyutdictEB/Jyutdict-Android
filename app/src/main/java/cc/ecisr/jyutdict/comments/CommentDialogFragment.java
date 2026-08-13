package cc.ecisr.jyutdict.comments;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Collections;

import cc.ecisr.jyutdict.MainActivity;
import cc.ecisr.jyutdict.R;
import cc.ecisr.jyutdict.auth.AuthRepository;
import cc.ecisr.jyutdict.utils.ToastUtil;

/** Public comment viewer plus authenticated create/delete controls. */
public final class CommentDialogFragment extends DialogFragment {
    public static final String RESULT_KEY = "comment_count_changed";
    public static final String RESULT_TYPE = "comment_type";
    public static final String RESULT_TARGET = "comment_target";
    public static final String RESULT_COUNT = "comment_count";

    private static final String ARG_TYPE = "type";
    private static final String ARG_TARGET = "target";
    private static final String ARG_LABEL = "label";

    private CommentRepository repository;
    private AuthRepository authRepository;
    private CommentAdapter adapter;
    private RecyclerView list;
    private ProgressBar progress;
    private TextView empty;
    private TextInputEditText input;
    private MaterialButton submit;
    private MaterialButton signIn;
    private String type;
    private String target;

    public static CommentDialogFragment newInstance(String type, String target, String label) {
        CommentDialogFragment fragment = new CommentDialogFragment();
        Bundle arguments = new Bundle();
        arguments.putString(ARG_TYPE, type);
        arguments.putString(ARG_TARGET, target);
        arguments.putString(ARG_LABEL, label);
        fragment.setArguments(arguments);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Bundle arguments = requireArguments();
        type = arguments.getString(ARG_TYPE, CommentRepository.TYPE_CHAR);
        target = arguments.getString(ARG_TARGET, "");
        String label = arguments.getString(ARG_LABEL, target);

        repository = new CommentRepository(requireContext());
        authRepository = AuthRepository.getInstance(requireContext());
        adapter = new CommentAdapter(this::confirmDelete);

        View view = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_comments, null, false);
        list = view.findViewById(R.id.comment_list);
        progress = view.findViewById(R.id.comment_loading);
        empty = view.findViewById(R.id.comment_empty);
        input = view.findViewById(R.id.comment_input);
        submit = view.findViewById(R.id.comment_submit);
        signIn = view.findViewById(R.id.comment_sign_in);

        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        list.setAdapter(adapter);
        submit.setOnClickListener(ignored -> submitComment());
        signIn.setOnClickListener(ignored -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).startGoogleSignIn(() -> {
                    updateAuthControls();
                    loadComments();
                });
            }
        });
        updateAuthControls();
        loadComments();

        return new MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.comment_dialog_title, label))
                .setView(view)
                .setPositiveButton(R.string.button_confirm, null)
                .create();
    }

    private void updateAuthControls() {
        boolean loggedIn = authRepository.isLoggedIn();
        input.setVisibility(loggedIn ? View.VISIBLE : View.GONE);
        submit.setVisibility(loggedIn ? View.VISIBLE : View.GONE);
        signIn.setVisibility(loggedIn ? View.GONE : View.VISIBLE);
    }

    private void loadComments() {
        progress.setVisibility(View.VISIBLE);
        empty.setVisibility(View.GONE);
        repository.getComments(type, target, (comments, errorMessage) -> {
            if (!isAdded()) return;
            progress.setVisibility(View.GONE);
            if (comments == null) {
                adapter.submit(Collections.emptyList(), authRepository.getCurrentUser(),
                        authRepository.isAdmin());
                empty.setText(getString(R.string.comment_load_failed, errorMessage));
                empty.setVisibility(View.VISIBLE);
                return;
            }
            adapter.submit(comments, authRepository.getCurrentUser(), authRepository.isAdmin());
            empty.setText(R.string.comment_empty);
            empty.setVisibility(comments.isEmpty() ? View.VISIBLE : View.GONE);
            publishCount();
        });
    }

    private void submitComment() {
        CharSequence value = input.getText();
        String content = value == null ? "" : value.toString().trim();
        if (content.isEmpty()) return;
        submit.setEnabled(false);
        repository.postComment(type, target, content, (success, errorMessage) -> {
            if (!isAdded()) return;
            submit.setEnabled(true);
            if (!success) {
                ToastUtil.msg(requireContext(), getString(R.string.comment_submit_failed, errorMessage));
                return;
            }
            input.setText("");
            loadComments();
        });
    }

    private void confirmDelete(Comment comment) {
        new MaterialAlertDialogBuilder(requireContext())
                .setMessage(R.string.comment_delete_confirm)
                .setNegativeButton(R.string.button_cancel, null)
                .setPositiveButton(R.string.comment_delete, (dialog, which) ->
                        repository.deleteComment(type, comment.id, (success, errorMessage) -> {
                            if (!isAdded()) return;
                            if (!success) {
                                ToastUtil.msg(requireContext(),
                                        getString(R.string.comment_delete_failed, errorMessage));
                                return;
                            }
                            loadComments();
                        }))
                .show();
    }

    private void publishCount() {
        Bundle result = new Bundle();
        result.putString(RESULT_TYPE, type);
        result.putString(RESULT_TARGET, target);
        result.putInt(RESULT_COUNT, adapter.activeCount());
        getParentFragmentManager().setFragmentResult(RESULT_KEY, result);
    }
}
