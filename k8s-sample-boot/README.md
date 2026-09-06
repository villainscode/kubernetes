# k8s-sample-boot

『Kubernetes 시작하기 2026』 실습용 Spring Boot 애플리케이션.

- Spring Boot **4.1.0** / JDK **17** / Gradle
- `GET /hello` → `Hello World! <버전> (Host = <파드명>)`
- `GET /actuator/prometheus` → 프로메테우스 형식 메트릭 (21 · 22장)

JDK 17 인 것은 교안 10.2절이 베이스 이미지를 `eclipse-temurin:17-jre` 로 지정하고
그 근거를 본문에서 설명하기 때문이다. 문서와 어긋나지 않게 그대로 둔다.

## 빌드

```bash
docker build -t k8s-sample-boot:v1 .
```

로컬에 JDK 가 없어도 된다. Dockerfile 앞단에 빌더 스테이지가 있어
컨테이너 안에서 Gradle 빌드까지 끝낸다.

## 이미지 세 벌

교안이 롤아웃과 관측 실습에 쓰는 이미지가 셋이다. 소스 한 곳씩만 다르다.

| 태그 | 무엇이 다른가 | 쓰는 장 |
|---|---|---|
| `v1` | `IndexController` 의 문자열이 `V1` | 10장 배포 |
| `v2` | 문자열이 `V2` (두 곳) | 11장 롤링 업데이트 · 롤백 |
| `v3` | 액추에이터 · 마이크로미터 의존성과 `application.properties` | 21 · 22장 관측 |

**저장소의 현재 상태가 `v3` 다.** 그대로 빌드하면 `/actuator/prometheus` 가 열린다.

```bash
# v3 (현재 상태)
docker build -t k8s-sample-boot:v3 .

# v1 · v2 — IndexController 의 문자열 두 곳을 바꾸고 빌드
#   "Hello World! V3"  →  "Hello World! V1"
#   log.info("##### getHello V3 = ...")  →  V1
docker build -t k8s-sample-boot:v1 .
```

## 클러스터에 올리기

```bash
kind load docker-image k8s-sample-boot:v3 --name study
kubectl set image deploy/k8s-sample-boot k8s-sample-boot=k8s-sample-boot:v3
```

## 계측

`application.properties` 가 세 가지를 정한다.

```properties
management.endpoints.web.exposure.include=health,info,prometheus
management.metrics.tags.application=k8s-sample-boot
management.metrics.distribution.percentiles-histogram.http.server.requests=true
```

`exposure.include` 를 `*` 로 두지 않는다. `env` 와 `configprops` 는 설정 값을 그대로
노출하고 `heapdump` 는 메모리 전체를 내려받게 한다.

히스토그램을 켜면 `histogram_quantile` 로 P99 를 구할 수 있는 대신
시계열이 버킷 수만큼 늘어난다. 공짜가 아니다 — 교안 21.6절에서 그 대가를 다룬다.
