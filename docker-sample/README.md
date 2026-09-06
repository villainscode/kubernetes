# docker-sample

『Docker 시작하기 2026』 실습용 Spring Boot 애플리케이션.

원문 교안이 쓰던 예제(Spring Boot 2.2.6 / Java 8, 2020년)를 **현행 스택으로 다시 쓴 것**이다.
구조와 엔드포인트는 원문의 자리를 그대로 지켰다.

- Spring Boot **4.1.0** / JDK **21** / Maven
- 의존성: `starter-web`, `starter-mustache`, `starter-jdbc`, `starter-actuator`, `mysql-connector-j`

## 엔드포인트

| 경로 | 무엇을 하나 | 쓰는 곳 |
|---|---|---|
| `GET /` | 머스태시 템플릿 렌더링. DB 를 건드리지 않는다 | 3 · 7장 |
| `GET /hello` | `Hello World! <버전> (Host = <호스트명>)` | 8 · 13 · 16장 |
| `GET /db` | `SELECT VERSION()` 을 실제로 실행한다 | 10 · 11장 |
| `GET /actuator/health` | 헬스 체크 | 13 · 16장 |

`/` 와 `/db` 가 나뉜 것이 이 예제의 핵심이다. `application.properties` 의 접속 주소가
`jdbc:mysql://localhost:3306` 으로 **일부러 틀려 있다** — 컨테이너 안에서 `localhost` 는
그 컨테이너 자신이므로 MySQL 컨테이너가 아니다. 그런데 `/` 는 멀쩡히 응답한다.
DB 를 건드리지 않기 때문이다. 설정이 틀렸는데도 "잘 도는 것처럼" 보이는 상황을
`/db` 로 드러낸다. 실행할 때 `SPRING_DATASOURCE_URL` 로 덮어쓰면 고쳐진다.

## Dockerfile 네 개

| 파일 | 방식 | 크기 | 쓰는 절 |
|---|---|---|---|
| `Dockerfile.naive` | 한 스테이지에서 빌드까지. JDK · 메이븐 · 소스가 전부 남는다 | **1.02GB** | 8.1 · 8.3 |
| `Dockerfile.jre` | 미리 빌드한 jar 만 복사 | **475MB** | 8.1 |
| `Dockerfile.multi` | 멀티스테이지 — 이미지 안에서 빌드하고 jar 만 꺼낸다 | **475MB** | 8.3 · 8.4 |
| `Dockerfile.arg` | 멀티스테이지 + `--build-arg APP_VERSION` 으로 v1 · v2 | 475MB | 8.6 |

```bash
docker build -f Dockerfile.naive -t docker-sample:naive .
docker build -f Dockerfile.multi -t docker-sample:multi .
docker images docker-sample
```

`Dockerfile.jre` 만 jar 가 먼저 필요하다. 로컬에 Maven 이 없으면 컨테이너로 빌드한다.

```bash
docker run --rm -v "$PWD":/src -w /src maven:3.9-eclipse-temurin-21 \
  mvn -B clean package -DskipTests
docker build -f Dockerfile.jre -t docker-sample:jre .
```

### 빌드 캐시 (8.4절)

같은 명령을 세 번 돌린 결과다. `pom.xml` 을 소스보다 먼저 복사한 한 줄이 이 차이를 만든다.

| 바꾼 것 | 시간 | 다시 실행된 단계 |
|---|---|---|
| 없음 | 1.63초 | 없다. 전부 캐시 |
| 소스 (`src/`) | 4.13초 | `COPY src` · `mvn package` · 실행 스테이지 |
| `pom.xml` | 20.74초 | 위 전부 + `mvn dependency:go-offline` |

수정 시각만 바꾸면(`touch`) 캐시는 깨지지 않는다. `COPY` 의 캐시 키는 파일 내용이다.

### 버전 두 벌 (8.6절)

```bash
docker build -f Dockerfile.arg -t docker-sample:v1 .
docker build -f Dockerfile.arg --build-arg APP_VERSION=v2 -t docker-sample:v2 .

docker run --rm docker-sample:v2 &   # /hello → Hello World! v2 (Host = ...)
```

## Compose (13장)

```bash
# depends_on 만 — probe 가 Exited (1) 로 끝난다
docker compose -f compose-bad.yaml -p dbad up -d
docker compose -f compose-bad.yaml -p dbad ps -a
docker compose -f compose-bad.yaml -p dbad down -v

# healthcheck + condition: service_healthy — 기다렸다가 app 이 뜬다
docker compose -f compose-good.yaml -p dgood up -d --build
curl localhost:8080/db      # DB OK — MySQL 8.4.11
docker compose -f compose-good.yaml -p dgood down -v
```

`compose-good.yaml` 은 `SPRING_DATASOURCE_URL` 을 `jdbc:mysql://mysql:3306/testdb` 로
덮어쓴다. 서비스 이름이 곧 DNS 이름이라는 것을 확인하는 자리다(10장).

## 취약점 스캔 (15장)

```bash
docker run --rm -v /var/run/docker.sock:/var/run/docker.sock \
  aquasec/trivy:0.74.0 image --scanners vuln --severity HIGH,CRITICAL docker-sample:multi
```

```
│ docker-sample:multi (ubuntu 24.04) │ ubuntu │  0  │
│ app/app.jar                        │  jar   │  3  │
Total: 3 (HIGH: 0, CRITICAL: 3)
```

읽는 법이 중요하다. **OS 계층은 0건이고 전부 애플리케이션 jar 안에서 나온다.**
최신 스택인데도 3건이 있다 — `tomcat-embed-core` 11.0.22 의 CVE 이고 11.0.25 에서 고쳐졌다.
"베이스 이미지를 올리면 해결된다"는 흔한 조언이 이 경우에는 틀리다는 뜻이고,
답은 애플리케이션 의존성을 올리는 것이다.

CI 게이트로 쓰려면 종료 코드를 받는다.

```bash
docker run --rm -v /var/run/docker.sock:/var/run/docker.sock \
  aquasec/trivy:0.74.0 image --scanners vuln --severity CRITICAL \
  --ignore-unfixed --exit-code 1 docker-sample:multi
echo $?
```

## 쿠버네티스로 (16장)

```bash
kind create cluster --config kind-1node.yaml
kind load docker-image docker-sample:multi --name docker-study

kubectl run app --image=docker-sample:multi --image-pull-policy=IfNotPresent --port=8080
kubectl get pod app -o wide
kubectl logs app

kind delete cluster --name docker-study
```

`--image-pull-policy=IfNotPresent` 가 반드시 필요하다. 기본 정책이 `Always` 로 잡히면
노드에 이미지가 있어도 레지스트리로 받으러 가서 `ImagePullBackOff` 가 된다.
