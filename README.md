# On-device Transportation Mode Detection (TMD)  

---

## 🚆 이동수단 데이터 수집 및 실시간 추론
<img src="https://github.com/user-attachments/assets/29c5c5f0-5399-475b-ac1e-6a4222635e5d">

---

## 📡 수집된 데이터 기반 실시간 On-device 학습
<img src="https://github.com/user-attachments/assets/388501bd-d59b-46fd-9564-1c9e4480df5a">

---

## 📑 프로젝트 소개
### 👤 실시간 이동수단 탐지 및 On-device 학습 애플리케이션
- 스마트폰 내 IMU 센서 및 GPS 데이터를 활용하여 **버스, 지하철, 자동차, 도보, 정지 등** 다양한 이동수단을 실시간으로 탐지
- 서버 의존도를 최소화하고 **On-device 모델 학습 및 추론**을 통해 개인정보 보호와 네트워크 독립성을 강화

> **특징**  
> - IMU 센서(Linear Accelerometer, Gyroscope, Magnetometer) 데이터를 활용한 **멀티 입력 기반 탐지 모델**  
> - TensorFlow Lite 기반 **경량화 CNN 모델**로 스마트폰에서 실시간 추론 가능  
> - 실시간 데이터 수집 후 즉시 On-device 학습 가능, **개인 맞춤형 탐지 정확도 향상**  
> - 네트워크 연결 없이도 안정적으로 동작하여 **오프라인 환경에서도 사용 가능**  

---

## 🛠 사용 기술 스택
- **Android** : Java  
- **Model** : Multi-input CNN (TensorFlow, TensorFlow Lite)  
- **Data** : IMU 센서(Linear Accelerometer, Gyroscope, Magnetometer)  
- **Processing** : 실시간 윈도우링, z-score 정규화  
