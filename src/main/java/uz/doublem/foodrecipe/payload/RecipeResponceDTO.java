package uz.doublem.foodrecipe.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class RecipeResponceDTO {
    private Integer id;
    private String title;
    private String description;
    private String imageUrl;
    private String videoUrl;
    private String author;
    private String authorLocation;
    private String authorImageUrl;
    private Integer authorId;
    private Boolean isFollow;
    private Boolean isSaved;
    private Double averageRating;
    private String cookingTime;
    private Long viewCount;

}
