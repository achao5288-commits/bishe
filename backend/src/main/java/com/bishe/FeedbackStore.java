package com.bishe;
import java.util.List;
interface FeedbackStore {
 FeedbackService.Exposure exposure(String userId,List<Long> ids);
 void feedback(String userId,long attractionId,String exposureId,int rating,String comment);
 FeedbackService.Summary summary();
 void delete(String userId);
}
