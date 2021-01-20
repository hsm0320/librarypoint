package librarypoint;

import javax.persistence.*;
import org.springframework.beans.BeanUtils;
import java.util.List;

@Entity
@Table(name="Notice_table")
public class Notice {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;
    private Long bookId;
    private Long memberId;
    private Long bookPoint;

    @PostPersist
    public void onPostPersist(){
        Noticed noticed = new Noticed();
        BeanUtils.copyProperties(this, noticed);
        noticed.publishAfterCommit();


    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    public Long getBookId() {
        return bookId;
    }

    public void setBookId(Long bookId) {
        this.bookId = bookId;
    }
    public Long getMemberId() {
        return memberId;
    }

    public void setMemberId(Long memberId) {
        this.memberId = memberId;
    }
    public Long getBookPoint() {
        return bookPoint;
    }

    public void setBookPoint(Long bookPoint) {
        this.bookPoint = bookPoint;
    }




}
