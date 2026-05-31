package com.backend.interactionservice.post.service;

import com.backend.commondataaccess.dto.OffsetPageResult;
import com.backend.commondataaccess.persistence.post.Post;
import com.backend.interactionservice.post.repository.PostElasticsearchRepository;
import com.backend.interactionservice.post.repository.PostQueryRepository;
import com.backend.interactionservice.post.repository.query.SearchCondition;
import com.backend.interactionservice.post.service.dto.PostDocument;
import com.backend.interactionservice.post.service.dto.PostListItem;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostElasticsearchRepository postElasticsearchRepository;
    private final PostQueryRepository postQueryRepository;

    public Post getPost(UUID id) {
        Optional<Post> post = postQueryRepository.fetchOneById(id);
        return post.get();
    }

    public OffsetPageResult<PostListItem> searchPosts(SearchCondition condition, UUID userId, Pageable pageable) {
        OffsetPageResult<PostDocument> esResult = postElasticsearchRepository.search(condition, pageable);

        List<PostDocument> documents = esResult.getItems();
        if (documents.isEmpty()) {
            return new OffsetPageResult<>(esResult.getTotalCount(), esResult.getPage(), esResult.getSize(), List.of());
        }

        List<UUID> postIds = documents.stream()
                                      .map(PostDocument::id)
                                      .toList();

        Set<UUID> likedIds = postQueryRepository.findLikedPostIds(userId, postIds);
        Set<UUID> bookmarkedIds = postQueryRepository.findBookmarkedPostIds(userId, postIds);

        return esResult.map(doc -> PostListItem.of(doc,
                                                   likedIds.contains(doc.id()),
                                                   bookmarkedIds.contains(doc.id())));
    }

    public Optional<PostListItem> searchPost(UUID postId, UUID userId) {
        return postElasticsearchRepository.findById(postId).map(doc -> {
            List<UUID> postIds = List.of(doc.id());
            boolean likesOfMe = !postQueryRepository.findLikedPostIds(userId, postIds).isEmpty();
            boolean bookmarksOfMe = !postQueryRepository.findBookmarkedPostIds(userId, postIds).isEmpty();
            return PostListItem.of(doc, likesOfMe, bookmarksOfMe);
        });
    }
}
