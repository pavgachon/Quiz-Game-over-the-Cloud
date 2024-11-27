import cv2
import numpy as np
from scipy.spatial import distance as dist

def measure_object(image_path, width_of_reference=3.5):
    """
    물체의 크기를 측정하는 함수
    :param image_path: 이미지 파일 경로
    :param width_of_reference: 참조 물체의 실제 너비 (cm)
    """
    # 이미지 읽기
    image = cv2.imread(image_path)
    # 이미지 크기 조정
    image = cv2.resize(image, (800, 600))
    # 원본 이미지 복사
    orig = image.copy()

    # 그레이스케일 변환
    gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
    # 가우시안 블러 적용
    blurred = cv2.GaussianBlur(gray, (7, 7), 0)
    # 엣지 검출
    edged = cv2.Canny(blurred, 50, 100)
    
    # 윤곽선 찾기
    contours, _ = cv2.findContours(edged.copy(), cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
    # 면적 기준으로 윤곽선 정렬
    contours = sorted(contours, key=cv2.contourArea, reverse=True)[:5]

    reference_object = None
    pixel_per_cm = None

    # 각 윤곽선 처리
    for c in contours:
        # 윤곽선 근사화
        peri = cv2.arcLength(c, True)
        approx = cv2.approxPolyDP(c, 0.02 * peri, True)

        # 참조 물체 찾기 (가장 왼쪽에 있는 물체로 가정)
        if reference_object is None:
            # 경계 상자 구하기
            box = cv2.minAreaRect(c)
            box = cv2.boxPoints(box)
            box = np.array(box, dtype="int")
            
            # 경계 상자 정렬
            box = order_points(box)
            
            # 참조 물체의 너비 계산 (픽셀)
            (tl, tr, br, bl) = box
            reference_width = dist.euclidean(tl, tr)
            
            # 픽셀 당 센티미터 비율 계산
            pixel_per_cm = reference_width / width_of_reference
            
            reference_object = (box, (0, 255, 0))
            continue

        # 다른 물체들의 크기 측정
        box = cv2.minAreaRect(c)
        box = cv2.boxPoints(box)
        box = np.array(box, dtype="int")
        box = order_points(box)
        
        # 물체의 치수 계산
        (tl, tr, br, bl) = box
        width_pixels = dist.euclidean(tl, tr)
        height_pixels = dist.euclidean(tl, bl)
        
        # 실제 크기로 변환 (센티미터)
        width_cm = width_pixels / pixel_per_cm
        height_cm = height_pixels / pixel_per_cm
        
        # 결과 표시
        mid_point = np.mean(box, axis=0).astype(int)
        cv2.putText(image, f"{width_cm:.1f}cm", 
                   (mid_point[0] - 40, mid_point[1]), 
                   cv2.FONT_HERSHEY_SIMPLEX, 0.65, (255, 0, 0), 2)
        cv2.putText(image, f"{height_cm:.1f}cm", 
                   (mid_point[0] + 40, mid_point[1]), 
                   cv2.FONT_HERSHEY_SIMPLEX, 0.65, (255, 0, 0), 2)
        
        # 윤곽선 그리기
        cv2.drawContours(image, [box.astype(int)], -1, (0, 0, 255), 2)

    # 결과 이미지 표시
    cv2.imshow("Image", image)
    cv2.waitKey(0)
    cv2.destroyAllWindows()

def order_points(pts):
    """
    경계 상자의 꼭지점들을 정렬하는 함수
    순서: 좌상단, 우상단, 우하단, 좌하단
    """
    rect = np.zeros((4, 2), dtype="float32")
    
    # 좌표 합으로 좌상단(min)과 우하단(max) 찾기
    s = pts.sum(axis=1)
    rect[0] = pts[np.argmin(s)]
    rect[2] = pts[np.argmax(s)]
    
    # 좌표 차로 우상단(min)과 좌하단(max) 찾기
    diff = np.diff(pts, axis=1)
    rect[1] = pts[np.argmin(diff)]
    rect[3] = pts[np.argmax(diff)]
    
    return rect

# 메인 실행 코드
if __name__ == "__main__":
    # 이미지 경로와 참조 물체의 실제 너비(cm)를 지정하여 실행
    image_path = "sample.jpg"
    reference_width = 3.5  # 참조 물체의 실제 너비 (cm)
    measure_object(image_path, reference_width)