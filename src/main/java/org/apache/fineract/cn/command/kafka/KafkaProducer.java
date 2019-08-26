package org.apache.fineract.cn.command.kafka;


import com.google.gson.Gson;
import org.apache.fineract.cn.command.util.CommandConstants;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

@Service
public class KafkaProducer {

    private final Logger logger;
    private final Gson gson;
    private final KafkaTemplate<String,String> kafkaTemplate;

    @Autowired
    public KafkaProducer(@Qualifier(CommandConstants.LOGGER_NAME) final Logger logger,
                         @Qualifier(CommandConstants.KAFKA_PRODUCER_CUSTOM) KafkaTemplate<String, String> kafkaTemplate,
                         Gson gson) {

        this.logger = logger;
        this.gson = gson;
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendMessage(String topic, Object message) {

        // TODO inverstigar reduccion de tamaño objecto
        String payload = this.gson.toJson(message);
        logger.info(String.format("$$ -> Producing json message --> %s", payload));

        ListenableFuture<SendResult<String, String>> future = this.kafkaTemplate.send(topic, payload);

        future.addCallback(new ListenableFutureCallback<SendResult<String, String>>() {

            @Override
            public void onSuccess(SendResult<String, String> result) {
                System.out.println("Sent message=[" + message + "] with offset=[" + result.getRecordMetadata().offset() + "]");
            }
            @Override
            public void onFailure(Throwable ex) {
                System.out.println("Unable to send message=[" + message + "] due to : " + ex.getMessage());
            }
        });
    }
}
