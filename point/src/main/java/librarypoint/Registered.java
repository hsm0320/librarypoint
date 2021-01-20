
package librarypoint;

public class Registered extends AbstractEvent {

    private Long id;
    private Long bookId;
    private Long memberId;
    private Long bookPoint;

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
