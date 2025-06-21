from locust import HttpUser, task, between

class WebsiteUser(HttpUser):
    wait_time = between(1, 3)  # 각 요청 사이 대기 시간(초)
    
    @task
    def index(self):
        self.client.get("/")
