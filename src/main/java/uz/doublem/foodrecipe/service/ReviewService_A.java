package uz.doublem.foodrecipe.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.doublem.foodrecipe.entity.LikeReview;
import uz.doublem.foodrecipe.entity.Recipe;
import uz.doublem.foodrecipe.entity.Review;
import uz.doublem.foodrecipe.entity.User;
import uz.doublem.foodrecipe.payload.*;
import uz.doublem.foodrecipe.repository.LikeReviewRepository;
import uz.doublem.foodrecipe.repository.RecipeRepository_A;
import uz.doublem.foodrecipe.repository.ReviewRepository_A;
import uz.doublem.foodrecipe.repository.SavedRecipeRepository_A;
import uz.doublem.foodrecipe.util.Util;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewService_A {


    private final ReviewRepository_A reviewRepository;
    private final RecipeRepository_A recipeRepository;
    private final SavedRecipeRepository_A recipeRepositoryA;
    private final LikeReviewRepository likeReviewRepository;


    public ResponseMessage review(Integer recipeId, User user, Integer page, Integer count) {
        if (!recipeRepository.existsById(recipeId)) {
            return Util.getResponseMes(false,"recipe not found",recipeId);
        }

        Pageable pageRequest = PageRequest.of(page, count);

        Page<Review> reviewPage = reviewRepository.findAllByRecipe_Id(recipeId, pageRequest);

        Long countSaved = recipeRepositoryA.countByRecipe_Id(recipeId);

        List<ReviewResponceDto> dtoList = getReviewDtoAs(reviewPage,user.getId());
        Long commentsCount = reviewRepository.countNonNullCommentsByUserAndRecipe(user.getId(), recipeId);
        ReviewDto_A responseReviewCommentPage = ReviewDto_A.builder()
                .recipeId(recipeId)
                .savedCount(countSaved)
                .commentsCount(commentsCount)
                .reviews(dtoList)
                .build();

        return ResponseMessage.builder()
                .text("success")
                .status(true)
                .data(responseReviewCommentPage)
                .build();
    }


    private List<ReviewResponceDto> getReviewDtoAs(Page<Review> reviewPage, Integer userId) {

        List<ReviewResponceDto> dtoList = reviewPage.stream()
                .map(review -> {
                    User user = review.getUser();
                    ReviewResponceDto dto = new ReviewResponceDto();
                    dto.setReviewId(review.getId());
                    dto.setUserDtoReview(new UserDtoReview(user.getId(),user.getName(), user.getImageUrl()));
                    dto.setComment(review.getComment());
                    dto.setCreated_at(review.getCreatedAt().toString());
                    dto.setLikes_count(review.getLikeCount());
                    dto.setDislikes_count(review.getDislikeCount());
                    Boolean b = likeReviewRepository.fetchLikeReviewById(userId, review.getId());
                    dto.setHasLikedUser(b);
                    return dto;
                })
                .collect(Collectors.toList());
        return dtoList;

    }

   @Transactional
   public ResponseMessage deleteComment (Integer reviewId, User user) {
       Optional<Review> byId = reviewRepository.findById(reviewId);
       if (byId.isPresent()) {
           Review review = byId.get();
           if (review.getComment()==null) {
              return Util.getResponseMes(false,"comment not found in review, return this id ",reviewId);
           }
           User user1 = review.getUser();
           User author = review.getRecipe().getAuthor();
           if (Objects.equals(user.getId(), user1.getId() )
                   || Objects.equals(user.getId(), author.getId())
                   || user.getId()==1
           ){
                  if (review.getRating()==null){
                   reviewRepository.delete(review);
                   return Util.getResponseMes(true,"delete review success",reviewId);
                  }else {
                      review.setComment(null);
                      reviewRepository.save(review);
                      return Util.getResponseMes(true,"delete only comment success",reviewId);
                  }
           }
       }
       return Util.getResponseMes(false,"review not found with this id",reviewId);
   }

    public ResponseMessage addReviewForRecipe(ReviewDto reviewDto, User user) {
        Integer recipeId = reviewDto.getRecipeId();
        Optional<Recipe> byId = recipeRepository.findById(recipeId);
        Review review = new Review();
        if (byId.isEmpty()) {
            return Util.getResponseMes(false,"Recipe not found",recipeId);
        }
        Optional<Review> optionReview = reviewRepository.findByRecipe_IdAndUser_id(recipeId, user.getId());
        if (optionReview.isPresent()){
            review = optionReview.get();
            if (reviewDto.getRating() != null) {
                review.setRating(reviewDto.getRating());
                reviewRepository.save(review);
                fixedAverageRateForRecipe(recipeId);
            }
            return Util.getResponseMes(true,"Rated to Recipe, and not create Review becouse Review Already created",recipeId);
        }
        if (reviewDto.getRating() != null) {
            review.setRating(reviewDto.getRating());
        }
        review.setRecipe(byId.get());
        review.setUser(user);
        review.setDislikeCount(0);
        review.setLikeCount(0);
        reviewRepository.save(review);
        fixedAverageRateForRecipe(recipeId);
        return Util.getResponseMes(true,"NEW Review create and rate set to Recipe ",review);
    }

    private void fixedAverageRateForRecipe(Integer recipeId) {
        Double averageRating = reviewRepository.findAverageRatingByRecipeId(recipeId);
        if (averageRating != null) {
            recipeRepository.updateAverageRating(recipeId,averageRating);
        }
    }

    public ResponseMessage addCommentForRecipe(ReviewDto reviewDto, User user) {
        Integer recipeId = reviewDto.getRecipeId();
        Optional<Recipe> byId = recipeRepository.findById(recipeId);
        Review review = new Review();
        if (byId.isEmpty()) {
            return Util.getResponseMes(false,"Recipe not found with this id: ",recipeId);
        }
        Optional<Review> optionReview = reviewRepository.findByRecipe_IdAndUser_id(recipeId, user.getId());
        if (optionReview.isPresent()){
            review = optionReview.get();
            if (reviewDto.getComment() != null) {
            review.setComment(reviewDto.getComment());
            }
            reviewRepository.save(review);
            return Util.getResponseMes(true,"commit set to Recipe, and not create Review because Review Already created, return reviewId: ",review.getId());
        }
        if (reviewDto.getComment() != null) {
            review.setComment(reviewDto.getComment());
        }
        review.setRecipe(byId.get());
        review.setUser(user);
        review.setDislikeCount(0);
        review.setLikeCount(0);
        reviewRepository.save(review);
        return Util.getResponseMes(true,"NEW Review create and rate set to Recipe, responce return reviewId ",review.getId());
    }

    @Transactional
    public ResponseMessage reactionToComment(ReviewLikeDtoAdd reviewLikeDtoAdd, User user) {
        Optional<Review> byId = reviewRepository.findById(reviewLikeDtoAdd.getReviewId());
        if (byId.isEmpty()) {
            return Util.getResponseMes(false,"review not found with this id: ",reviewLikeDtoAdd.getReviewId());
        } else if (byId.get().getComment() == null) {
            return Util.getResponseMes(false,"review has not comment so you don't reacted this review : ",reviewLikeDtoAdd.getReviewId());
        }
        Review review = byId.get();
        Boolean hasLikedNew = reviewLikeDtoAdd.getHasLiked();
        Optional<LikeReview> likeReviewOptional = likeReviewRepository.findByReview_IdAndUser_Id(reviewLikeDtoAdd.getReviewId(), user.getId());
        if (likeReviewOptional.isEmpty()) {
            LikeReview newLikeReview = createNewLikeReview(review, user, hasLikedNew);
            return Util.getResponseMes(true,
                    "Set reaction for Recipe, create like-review",
                    LikeReviewResponseDto.builder()
                            .Id(newLikeReview.getId())
                            .reviewId(newLikeReview.getReview().getId())
                            .userId(newLikeReview.getUser().getId())
                            .isLike(newLikeReview.getIsLike())
                            .build());
        }
        LikeReview likeReview = likeReviewOptional.get();
        Boolean isLikeOld = likeReview.getIsLike();
        if (hasLikedNew.equals(isLikeOld)) {
            deleteLikeReview(likeReview.getId());
            changeCountReaction(review, hasLikedNew, isLikeOld);
            return Util.getResponseMes(true, "Removed reaction successfully, deleted review like", reviewLikeDtoAdd);
        } else {
            likeReview.setIsLike(hasLikedNew);
            likeReviewRepository.save(likeReview);
            changeCountReaction(review, hasLikedNew, isLikeOld);
            return Util.getResponseMes(true, "Changed reaction for review without creating new LikeReview", reviewLikeDtoAdd);
        }
    }
    private LikeReview createNewLikeReview(Review review, User user, Boolean hasLikedNew) {
        LikeReview likeReview = new LikeReview();
        likeReview.setReview(review);
        likeReview.setUser(user);
        likeReview.setIsLike(hasLikedNew);
        likeReviewRepository.save(likeReview);
        changeCountReaction(review, hasLikedNew, null);
        return likeReview;
    }

    private void changeCountReaction(Review review, Boolean hasLikedNew, Boolean isLikedOld) {
            if (isLikedOld == null) {
                if (hasLikedNew) {
                    review.setLikeCount(review.getLikeCount() + 1);
                } else {
                    review.setDislikeCount(review.getDislikeCount() + 1);
                }
            } else if (hasLikedNew.equals(isLikedOld)) {
                if (hasLikedNew) {
                    review.setLikeCount(review.getLikeCount() - 1);
                } else {
                    review.setDislikeCount(review.getDislikeCount() - 1);
                }
            } else {
                if (hasLikedNew) {
                    review.setLikeCount(review.getLikeCount() + 1);
                    review.setDislikeCount(review.getDislikeCount() - 1);
                } else {
                    review.setLikeCount(review.getLikeCount() - 1);
                    review.setDislikeCount(review.getDislikeCount() + 1);
                }
            }
            reviewRepository.save(review);
    }

    @Transactional
    protected void deleteLikeReview(Integer id) {
        likeReviewRepository.deleteById(id);
    }

    public ResponseMessage getReviewRate(Integer id, User user) {
        Optional<Review> review = reviewRepository.findByRecipe_IdAndUser_id(id, user.getId());
        if (review.isEmpty()) {
            return Util.getResponseMes(false,"review not found with this recipe Id : ",id);
        }
        return Util.getResponseMes(true,"get review rate successfully ",review.get().getRating());
    }
}
