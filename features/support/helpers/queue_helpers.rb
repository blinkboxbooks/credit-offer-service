module KnowsAboutQueueHelpers
  @@queues_map = {}

  def create_queue(queue_name, is_passive = false)
    @@queues_map[queue_name] = $amqp_ch.queue(queue_name, durable: true, passive: is_passive)
    @@queues_map[queue_name]
  end

  def purge_queues
    @@queues_map.each do |_queue_name, queue|
      purge_queue(queue)
      Wrong.eventually(:timeout => 1) {pop_message_from_queue(_queue_name, 0.1) == nil}
    end
  end

  def purge_queue(queue)
    queue.purge
  end

  def publish_message_to_queue(queue_name, msg_payload, is_persistent = true)
    @@queues_map[queue_name].publish(msg_payload, persistent: is_persistent)
  end

  def publish_message_to_exchange(exchange_name, message, routing_key)
    $amqp_ch.basic_publish(message, exchange_name, routing_key)
  end

  def create_exchange(exchange_name, type='fanout')
    $amqp_ch.exchange_declare(exchange="#{exchange_name}", type, :durable => true)
  end

  def queue(queue_name)
    @@queues_map[queue_name]
  end

  def is_queue_empty(queue_name)
    queue = @@queues_map[queue_name]

    received_message = nil
    queue.pop do |_delivery_info, _metadata, payload|
      received_message = payload
    end
    !received_message
  end

  POLLING_INTERVAL_SECONDS = 0.1

  def pop_message_from_queue(queue_name, timeout = 2)
    queue = @@queues_map[queue_name]

    unless queue
      raise "queue with name: #{queue_name} not found"
    end

    time_limit = Time.now + timeout
    received_message = nil

    until received_message || Time.now >= time_limit
      queue.pop do |_delivery_info, _metadata, payload|
        received_message = payload
        sleep POLLING_INTERVAL_SECONDS
      end
    end
    received_message
   end

  def close_connection
    $amqp_conn.close
  end

end
World(KnowsAboutQueueHelpers)
