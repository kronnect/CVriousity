#include <jni.h>
#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/features2d/features2d.hpp>
#include <opencv2/calib3d/calib3d.hpp>
#include <opencv2/highgui/highgui.hpp>
#include <vector>
#include <iostream>
#include <android/log.h>

using namespace std;
using namespace cv;

#define LOG_TAG "TestApp_JNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

extern "C" {

#define MAXIMAGES 100
#define MAXMETHODS 5

vector<KeyPoint> imageTrainKeypoints[MAXMETHODS][MAXIMAGES];
Mat imageTrainDescriptors[MAXMETHODS][MAXIMAGES];
std::vector<Point2f> imageTrainObjCorners[MAXMETHODS][MAXIMAGES];
int imageTrainIndex[MAXMETHODS];

bool busy = false;
int sceneHeight = 0, sceneWidth = 0;
Scalar colorWhite = Scalar::all(255);
vector<KeyPoint> keypoints;
Mat descriptors;
CvFont font = cvFont(20, 2);

std::vector<Point2f> scene_corners(4);
double svd_expr, dt;
int STAGE;
Mat* pMatGr;

std::vector<DMatch> good_matches;
Point p0(0, 0), p1(0, 0), p2(0, 0), p3(0, 0);

MserFeatureDetector MSERfeatureDetector; //(50);
FastFeatureDetector FASTfeatureDetector(50);
OrbFeatureDetector ORBfeatureDetector; //(50); //(2000);//, cm);
OrbDescriptorExtractor ORBfeatureExtractor;
GoodFeaturesToTrackDetector GFTTfeatureDetector;
Ptr<DescriptorExtractor> BRIEFfeatureExtractor = DescriptorExtractor::create(
		"BRIEF");
Ptr<DescriptorExtractor> FREAKfeatureExtractor = DescriptorExtractor::create(
		"FREAK");

void extractFeatures(int viewMode, Mat image, vector<KeyPoint>& keypoints,
		Mat& descriptors) {
	switch (viewMode) {
	case 0:
		FASTfeatureDetector.detect(image, keypoints);
		BRIEFfeatureExtractor->compute(image, keypoints, descriptors);
		break;
	case 1:
		ORBfeatureDetector.detect(image, keypoints);
		ORBfeatureExtractor.compute(image, keypoints, descriptors);
		break;
	case 2:
		FASTfeatureDetector.detect(image, keypoints);
		FREAKfeatureExtractor->compute(image, keypoints, descriptors);
		break;
	case 3:
		MSERfeatureDetector.detect(image, keypoints);
		FREAKfeatureExtractor->compute(image, keypoints, descriptors);
		break;
	case 4:
		GFTTfeatureDetector.detect(image, keypoints);
		FREAKfeatureExtractor->compute(image, keypoints, descriptors);
		break;
	}
}

void crossCheckMatching(Ptr<DescriptorMatcher>& descriptorMatcher,
		const Mat& descriptors1, const Mat& descriptors2,
		vector<DMatch>& filteredMatches12, int knn = 1) {
	filteredMatches12.clear();
	vector<vector<DMatch> > matches12, matches21;
	descriptorMatcher->knnMatch(descriptors1, descriptors2, matches12, knn);
	descriptorMatcher->knnMatch(descriptors2, descriptors1, matches21, knn);
	for (size_t m = 0; m < matches12.size(); m++) {
		bool findCrossCheck = false;
		for (size_t fk = 0; fk < matches12[m].size(); fk++) {
			DMatch forward = matches12[m][fk];

			for (size_t bk = 0; bk < matches21[forward.trainIdx].size(); bk++) {
				DMatch backward = matches21[forward.trainIdx][bk];
				if (backward.trainIdx == forward.queryIdx) {
					filteredMatches12.push_back(forward);
					findCrossCheck = true;
					break;
				}
			}
			if (findCrossCheck)
				break;
		}
	}
}

JNIEXPORT void JNICALL Java_com_ramirooliva_cvriousity_CatalogManager_ResetTrain(
		JNIEnv*, jobject) {
	for (int k = 0; k < MAXMETHODS; k++) {
		imageTrainIndex[k] = -1;
	}
}

JNIEXPORT void JNICALL Java_com_ramirooliva_cvriousity_CatalogManager_TrainImage(
		JNIEnv* env, jobject, int viewMode, jstring jfilename,
		int heightPreview) {
	LOGI("Training called (ooo)....");
	try {
		Mat imageTrain;

		const char *imageTrainFile = env->GetStringUTFChars(jfilename, NULL);
		Mat aux = imread(imageTrainFile, CV_LOAD_IMAGE_GRAYSCALE);
		double SCREEN_FACTOR = ((double) heightPreview / (double) aux.rows);
		resize(aux, imageTrain, Size(), SCREEN_FACTOR, SCREEN_FACTOR,
				INTER_AREA);


		imageTrainIndex[viewMode]++;
		extractFeatures(viewMode, imageTrain,
				imageTrainKeypoints[viewMode][imageTrainIndex[viewMode]],
				imageTrainDescriptors[viewMode][imageTrainIndex[viewMode]]);
		std::vector<Point2f> objCorners = std::vector<Point2f>(4);
		objCorners[0] = cvPoint(0, 0);
		objCorners[1] = cvPoint(imageTrain.cols, 0);
		objCorners[2] = cvPoint(imageTrain.cols, imageTrain.rows);
		objCorners[3] = cvPoint(0, imageTrain.rows);
		imageTrainObjCorners[viewMode][imageTrainIndex[viewMode]] = objCorners;
		imageTrain.release();
		aux.release();

		ostringstream os;
		os << "Training finished.... " << imageTrainKeypoints[viewMode][imageTrainIndex[viewMode]].size() << " keypoints.";
		LOGI(os.str().c_str());

		env->ReleaseStringUTFChars(jfilename, imageTrainFile);
	} catch (Exception const &ex) {
		LOGI(ex.msg.c_str());
	}

}

bool checkAgainstTrained(int train, int viewMode) {

	bool homogramma_is_good = false;
	int height, width;

	try {
		STAGE = 3;
		vector<int> queryIdxs(good_matches.size()), trainIdxs(
				good_matches.size());
		for (size_t i = 0; i < good_matches.size(); i++) {
			queryIdxs[i] = good_matches[i].queryIdx;
			trainIdxs[i] = good_matches[i].trainIdx;
		}
		vector<Point2f> points1;
		KeyPoint::convert(imageTrainKeypoints[viewMode][train], points1,
				queryIdxs);
		vector<Point2f> points2;
		KeyPoint::convert(keypoints, points2, trainIdxs);
		vector<char> matchesMask(good_matches.size(), 0);
		int inliners_matches = 0;

		Mat H;
		H = findHomography(Mat(points1), Mat(points2), CV_RANSAC, 5);

		STAGE = 6;
		Mat points1t;
		perspectiveTransform(Mat(points1), points1t, H);
//		for (size_t i1 = 0; i1 < points1.size(); i1++) {
//			if (norm(points2[i1] - points1t.at<Point2f>((int) i1, 0)) < 5) { // inlier
//				matchesMask[i1] = 1;
//				inliners_matches++;
//			}
//		}
		homogramma_is_good = false;
		if (!H.empty()) {
			STAGE = 7;
			dt = determinant(H);
			Mat w;

			if (dt > 0.2 && dt < 2.5) {
				SVD::compute(H, w);
				svd_expr = w.at<double>(0, 0) / w.at<double>(0, 2);
				homogramma_is_good = svd_expr < 500000;
			}

		}

		if (homogramma_is_good == true) {
			std::vector<Point2f> obj_corners =
					imageTrainObjCorners[viewMode][train];
			STAGE = 8;
			perspectiveTransform(obj_corners, scene_corners, H);
			p0.x = scene_corners[0].x; // / SCALE_SCENE;
			p0.y = scene_corners[0].y; // / SCALE_SCENE;
			p1.x = scene_corners[1].x; // / SCALE_SCENE;
			p1.y = scene_corners[1].y; // / SCALE_SCENE;
			p2.x = scene_corners[2].x; // / SCALE_SCENE;
			p2.y = scene_corners[2].y; // / SCALE_SCENE;
			p3.x = scene_corners[3].x; // / SCALE_SCENE;
			p3.y = scene_corners[3].y; // / SCALE_SCENE;

			return true;

		}

	} catch (Exception const &ex) {
		ostringstream ss;
		ss << ex.msg;
		string lastException = ss.str();
		putText(*pMatGr, lastException, cvPoint(30, 40), 3, 0.3, colorWhite, 1);
		if (lastException.length() > 40) {
			putText(*pMatGr, lastException.substr(40), cvPoint(30, 60), 3, 0.3,
					colorWhite, 1);
		}
	}
	return false;

}

JNIEXPORT void JNICALL Java_com_ramirooliva_cvriousity_CameraView_FindFeatures(
		JNIEnv* env, jobject, jlong addrGray, int viewMode, jintArray polygon) {

	pMatGr = (Mat*) addrGray;

	try {
		if (busy) {
			return;
		}
		busy = true;

		Mat aux = (*pMatGr);
		sceneHeight = aux.rows;
		sceneWidth = aux.cols;
		busy = false;
		return;
		extractFeatures(viewMode, aux, keypoints, descriptors);
		if (keypoints.size() > 20) {
			int bestMatches[10];
			int bestIndex = -1;
			std::vector<DMatch> bestGoodMatches[10];
			int bestGoodMatchesCount[10];

			// Check agains trained image
			Ptr<DescriptorMatcher> matcher = new BFMatcher(NORM_HAMMING2, true);
			std::vector<DMatch> matches;
			for (int train = 0; train <= imageTrainIndex[viewMode]; train++) {
				bestGoodMatchesCount[train] = 0;
				crossCheckMatching(matcher,
						imageTrainDescriptors[viewMode][train], descriptors,
						matches, 1);
				if (matches.size() > 10) {
					bestIndex++;
					bestMatches[bestIndex] = train;
					bestGoodMatches[bestIndex] = matches;
					bestGoodMatchesCount[bestIndex] = matches.size();
				}
			}
			for (int k = 0; k <= bestIndex; k++) {
				int maxMatches = 0;
				int bestMatch = -1;
				for (int j = 0; j <= bestIndex; j++) {
					if (bestGoodMatchesCount[j] > maxMatches) {
						maxMatches = bestGoodMatchesCount[j];
						bestMatch = j;
					}
				}
				if (bestMatch >= 0) {
					good_matches = bestGoodMatches[bestMatch];
					bestGoodMatchesCount[bestMatch] = 0;
					if (checkAgainstTrained(bestMatch, viewMode)) {
						jclass rectClass = env->GetObjectClass(polygon);
						jint *sarr = env->GetIntArrayElements(polygon, NULL);
						sarr[0] = p0.x;
						sarr[1] = p0.y;
						sarr[2] = p1.x;
						sarr[3] = p1.y;
						sarr[4] = p2.x;
						sarr[5] = p2.y;
						sarr[6] = p3.x;
						sarr[7] = p3.y;
						sarr[8] = bestMatch;
						env->ReleaseIntArrayElements(polygon, sarr, JNI_COMMIT);
						k = bestIndex; // makes loop exit
					}
				}
			}
		}

	} catch (Exception const &ex) {
		ostringstream ss;
		ss << ex.msg;
		string lastException = ss.str();
		putText(*pMatGr, lastException, cvPoint(30, 40), 3, 0.3, colorWhite, 1);
		if (lastException.length() > 40) {
			putText(*pMatGr, lastException.substr(40), cvPoint(30, 60), 3, 0.3,
					colorWhite, 1);
		}
	}
	busy = false;
}

}
