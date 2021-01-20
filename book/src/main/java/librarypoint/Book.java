package librarypoint;

import javax.persistence.*;

import librarypoint.BookApplication;
import librarypoint.StatusUpdated;
import org.springframework.beans.BeanUtils;
import java.util.List;

@Entity
@Table(name="Book_table")
public class Book {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;
    private String bookStatus;
    private Long memberId;
    private Long rendtalId;
    private String bookReview;

    @PostPersist
    public void onPostPersist(){
        //북리뷰가 없으면 예약
        if (this.bookReview.equals(""))
        {
            System.out.println("예약");
            // 예약
            StatusUpdated statusUpdated = new StatusUpdated();
            BeanUtils.copyProperties(this, statusUpdated);
            statusUpdated.publishAfterCommit();

        }
        //북리뷰가 있으면 리뷰
        else
        {   System.out.println("##### 리뷰입니다.");
            Reviewed reviewed = new Reviewed();
            BeanUtils.copyProperties(this, reviewed);
            reviewed.publishAfterCommit();

            //Following code causes dependency to external APIs
            // it is NOT A GOOD PRACTICE. instead, Event-Policy mapping is recommended.
            librarypoint.external.Point point = new librarypoint.external.Point();
            // mappings goes here
            point.setMemberId(this.memberId);
            point.setBookId(this.id);
            point.setBookPoint((long)100);

            BookApplication.applicationContext.getBean(librarypoint.external.PointService.class)
                    .registership(point);



        }

    }

    @PostUpdate
    public void onPostUpdate() {
        // 예약취소, 대여,반납
        StatusUpdated statusUpdated = new StatusUpdated();
        BeanUtils.copyProperties(this, statusUpdated);
        statusUpdated.publishAfterCommit();
    }


    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }

    public String getBookStatus() {
        return bookStatus;
    }
    public void setBookStatus(String bookStatus) {
        this.bookStatus = bookStatus;
    }

    public Long getMemberId() {
        return memberId;
    }
    public void setMemberId(Long memberId) {
        this.memberId = memberId;
    }

    public Long getRendtalId() {
        return rendtalId;
    }
    public void setRendtalId(Long rendtalId) {
        this.rendtalId = rendtalId;
    }


    public String getBookReview() {
        return bookReview;
    }

    public void setBookReview(String bookReview) {
        this.bookReview = bookReview;
    }
}
