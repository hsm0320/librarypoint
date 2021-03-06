package librarypoint;

import librarypoint.config.kafka.KafkaProcessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
public class PolicyHandler{
    @StreamListener(KafkaProcessor.INPUT)
    public void onStringEventListener(@Payload String eventString){

    }
    @Autowired
    NoticeRepository noticeRepository;
    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverRegistered_(@Payload Registered registered){

        if(registered.isMe()){
            System.out.println("##### 메시지 발송  : ");

            Notice notice = new Notice();

            notice.setId(registered.getId());
            notice.setMemberId(registered.getMemberId());
            notice.setBookId(registered.getBookId());
            notice.setBookPoint(registered.getBookPoint());

            noticeRepository.save(notice);
        }
    }

}
