package cc.ecisr.jyutdict.comments;

import android.app.Dialog;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Collections;

import cc.ecisr.jyutdict.R;
import cc.ecisr.jyutdict.auth.AuthRepository;
import cc.ecisr.jyutdict.databinding.DialogCommentsBinding;
import cc.ecisr.jyutdict.utils.MotionUtil;
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
    private DialogCommentsBinding binding;
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

        binding = DialogCommentsBinding.inflate(getLayoutInflater());
        binding.commentList.setLayoutManager(new LinearLayoutManager(requireContext()));
        MotionUtil.configureItemAnimator(binding.commentList);
        binding.commentList.setAdapter(adapter);
        binding.commentSubmit.setOnClickListener(ignored -> submitComment());
        updateAuthControls();
        loadComments();

        return new MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.comment_dialog_title, label))
                .setView(binding.getRoot())
                .setPositiveButton(R.string.button_confirm, null)
                .create();
    }

    private void updateAuthControls() {
        DialogCommentsBinding views = binding;
        if (views == null) return;
        boolean loggedIn = authRepository.isLoggedIn();
        MotionUtil.beginLayoutTransition(views.commentStateContainer);
        views.commentInput.setVisibility(loggedIn ? View.VISIBLE : View.GONE);
        views.commentSubmit.setVisibility(loggedIn ? View.VISIBLE : View.GONE);
    }

    private void loadComments() {
        DialogCommentsBinding views = binding;
        if (views == null) return;
        MotionUtil.beginLayoutTransition(views.commentStateContainer);
        views.commentLoading.setVisibility(View.VISIBLE);
        views.commentEmpty.setVisibility(View.GONE);
        repository.getComments(type, target, (comments, errorMessage) -> {
            DialogCommentsBinding callbackViews = binding;
            if (!isAdded() || callbackViews == null) return;
            MotionUtil.beginLayoutTransition(callbackViews.commentStateContainer);
            callbackViews.commentLoading.setVisibility(View.GONE);
            if (comments == null) {
                adapter.submit(Collections.emptyList(), authRepository.getCurrentUser(),
                        authRepository.isAdmin());
                callbackViews.commentEmpty.setText(
                        getString(R.string.comment_load_failed, errorMessage));
                callbackViews.commentEmpty.setVisibility(View.VISIBLE);
                return;
            }
            adapter.submit(comments, authRepository.getCurrentUser(), authRepository.isAdmin());
            callbackViews.commentEmpty.setText(R.string.comment_empty);
            callbackViews.commentEmpty.setVisibility(
                    comments.isEmpty() ? View.VISIBLE : View.GONE);
            publishCount();
        });
    }

    private void submitComment() {
        DialogCommentsBinding views = binding;
        if (views == null) return;
        CharSequence value = views.commentInput.getText();
        String content = value == null ? "" : value.toString().trim();
        if (content.isEmpty()) return;
        views.commentSubmit.setEnabled(false);
        repository.postComment(type, target, content, (success, errorMessage) -> {
            DialogCommentsBinding callbackViews = binding;
            if (!isAdded() || callbackViews == null) return;
            callbackViews.commentSubmit.setEnabled(true);
            if (!success) {
                ToastUtil.msg(requireContext(), getString(R.string.comment_submit_failed, errorMessage));
                return;
            }
            callbackViews.commentInput.setText("");
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

    @Override
    public void onDestroyView() {
        if (binding != null) binding.commentList.setAdapter(null);
        binding = null;
        super.onDestroyView();
    }
}
