# kubernetes — 도커 & 쿠버네티스 교재 실습 예제

『Docker 시작하기 2026』과 『Kubernetes 시작하기 2026』 두 교안이 쓰는 Spring Boot
애플리케이션과 실습 파일이다. 교안의 코드 블록이 정본이고 이 저장소는 그 실행 가능한 형태다.

## 두 애플리케이션

| 디렉터리 | 스택 | 쓰는 곳 |
|---|---|---|
| [`docker-sample/`](docker-sample) | Spring Boot 4.1.0 · JDK 21 · Maven | 도커 교재 8 · 12 · 13 · 14 · 15 · 16장 |
| [`k8s-sample-boot/`](k8s-sample-boot) | Spring Boot 4.1.0 · JDK 17 · Gradle | 쿠버네티스 교재 10 ~ 22장 |

둘 다 같은 일을 한다 — `/hello` 가 버전과 호스트명을 돌려준다. 어느 컨테이너·파드가
응답했는지 눈으로 구분하기 위한 것이고, 롤링 업데이트·로드밸런싱·카나리 실습이 전부
이 한 줄을 대조해서 판정한다.

나뉜 이유는 빌드 도구와 실습 범위다. 도커 교재는 메이븐 멀티스테이지와 이미지 크기
비교가 주제라 Maven 쪽이고, 쿠버네티스 교재는 액추에이터·프로메테우스 계측까지 가므로
Gradle 쪽이다. 한쪽만 필요하면 그 디렉터리만 받아도 된다.

## 빠른 시작

```bash
git clone https://github.com/villainscode/kubernetes.git
cd kubernetes

# 도커 교재
cd docker-sample
docker build -f Dockerfile.multi -t docker-sample:multi .
docker run --rm -p 8080:8080 docker-sample:multi
curl localhost:8080/hello

# 쿠버네티스 교재
cd ../k8s-sample-boot
docker build -t k8s-sample-boot:v1 .
```

로컬에 JDK 나 Maven 이 없어도 된다. 두 Dockerfile 모두 빌더 스테이지에서 빌드까지 끝낸다.

## 버전 정책

- 이미지 태그는 **마이너 버전까지 고정**한다. `latest` 는 쓰지 않는다 — 도구 이미지도 예외가 아니다
- 베이스 이미지: `eclipse-temurin:21-jre-noble` · `eclipse-temurin:17-jre`
- 실행 사용자는 숫자 UID(`USER 10001`). 쿠버네티스의 `runAsNonRoot` 검사가 숫자를 요구한다

## 검증 환경

macOS 15 (Apple Silicon) · Docker Engine 29.2.1 · Docker Compose v5.1.0 ·
Maven 3.9 · MySQL 8.4.11 · Trivy 0.74.0 · kind v0.33.0 · Kubernetes 1.37.0 ·
2026-09-06 측정.

이미지 크기 · 빌드 시간 · 취약점 개수는 환경과 날짜에 따라 다르다.
숫자 자체보다 구성 사이의 차이를 본다.
