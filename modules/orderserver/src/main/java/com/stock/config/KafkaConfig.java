package com.stock.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

@Configuration
@PropertySource(value = { "classpath:/kafkaConn.properties" })
public class KafkaConfig {

	@Value("${spring.kafka.bootstrap-servers}")
	private String bootstrapAddress;

	@Autowired
	private ConfigurableEnvironment env;

	/**
	 * Configure Kafka Admin.
	 * 
	 * @return
	 */
	@Bean
	public KafkaAdmin configureKafkaAdmin() {
		Map<String, Object> configs = new HashMap<String, Object>();
		configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapAddress);
		return new KafkaAdmin(configs);
	}

	/**
	 * Find property source containing kafka in its name & identify properties
	 * starting with topicDef. Extract topicName along with other variables to
	 * create topic.
	 * 
	 * @return
	 * @throws Exception
	 */
	@Bean
	public List<NewTopic> createTopics() throws Exception {

		List<NewTopic> output = new ArrayList<NewTopic>();

		/*
		 * Optional<Set<Entry<Object, Object>>> propertySet =
		 * env.getPropertySources().stream() .filter(p ->
		 * p.getName().contains("kafkaConn")).map(p -> (java.util.Properties)
		 * p.getSource()) .map(p -> p.entrySet()).findFirst();
		 * 
		 * if (propertySet.isPresent()) { Set<Entry<Object, Object>> propSet =
		 * propertySet.get(); Iterator<Entry<Object, Object>> iter = propSet.iterator();
		 * while (iter.hasNext()) { Entry<Object, Object> entry = iter.next(); String
		 * key = (String) entry.getKey(); String value = (String) entry.getValue();
		 * 
		 * if (key.startsWith("topicDef")) { String topicName =
		 * key.substring("topicDef".length() + 1); String[] valueSpl = value.split(",");
		 * Integer partNo = Integer.parseInt(valueSpl[0]); Integer replFactor =
		 * Integer.parseInt(valueSpl[1]); NewTopic nt = createTopic(topicName, partNo,
		 * replFactor.shortValue()); output.add(nt); } } } else { throw new
		 * Exception("No Kafka topics found!"); }
		 */
		
		NewTopic nt = createTopic("IN.ORDER.BUY", 1, (short) 1);
		output.add(nt);

		return output;
	}

	/**
	 * Create new topic.
	 * 
	 * @param topicName
	 * @param partitionNumber
	 * @param replicationFactor
	 * @return
	 */
	@Bean
	public NewTopic createTopic(String topicName, int partitionNumber, short replicationFactor) {
		return new NewTopic(topicName, partitionNumber, replicationFactor);
	}

	@Bean
	public ProducerFactory<String, String> producerFactory() {
		Map<String, Object> configProps = new HashMap<>();
		configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapAddress);
		configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
		configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
		return new DefaultKafkaProducerFactory<>(configProps);
	}

	@Bean
	public KafkaTemplate<String, String> kafkaTemplate() {
		return new KafkaTemplate<>(producerFactory());
	}
}
