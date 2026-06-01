package com.backend.interactionservice.post.service;

import com.backend.commondataaccess.dto.OffsetPageResult;
import com.backend.commondataaccess.exception.NotFoundException;
import com.backend.commondataaccess.persistence.post.Post;
import com.backend.interactionservice.post.repository.PostDocumentElasticsearchRepository;
import com.backend.interactionservice.post.repository.PostQueryRepository;
import com.backend.interactionservice.post.repository.query.SearchCondition;
import com.backend.interactionservice.post.service.dto.PostDocument;
import com.backend.interactionservice.post.service.dto.PostListItem;
import com.backend.interactionservice.post.service.validator.PostDocumentValidator;
import com.backend.interactionservice.post.service.validator.PostValidator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostDocumentElasticsearchRepository postDocumentElasticsearchRepository;
    private final PostQueryRepository postQueryRepository;

    public Post getPost(UUID id) {
        PostValidator.validateId(id);
        return postQueryRepository.fetchOneById(id)
                                  .orElseThrow(() -> new NotFoundException("존재하지 않는 post입니다. id: " + id));
    }

    public OffsetPageResult<PostListItem> searchPosts(SearchCondition condition, UUID userId, Pageable pageable) {
        OffsetPageResult<PostDocument> esResult = postDocumentElasticsearchRepository.search(condition, pageable);

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

    public PostListItem searchPost(UUID postId, UUID userId) {
        PostValidator.validateId(postId);

        PostDocument postDocument = PostDocumentValidator.getPostDocumentOrThrow(postId, postDocumentElasticsearchRepository::fetchOneById);

        List<UUID> postIds = List.of(postDocument.id());
        boolean likesOfMe = !postQueryRepository.findLikedPostIds(userId, postIds).isEmpty();
        boolean bookmarksOfMe = !postQueryRepository.findBookmarkedPostIds(userId, postIds).isEmpty();
        return PostListItem.of(postDocument, likesOfMe, bookmarksOfMe);
    }
}
