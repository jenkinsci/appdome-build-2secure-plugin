import os
import boto3
import requests

def main():
    aws_access_key_id = os.environ.get('AWS_ACCESS_KEY_ID')
    aws_secret_access_key = os.environ.get('AWS_SECRET_ACCESS_KEY')
    aws_default_region = 'eu-central-1'
    bucket_name = "appdome-automation-vanilla-apps"
    objects = {
        'aab_app': 'Thomas/PipelineFiles/Apps/FileFinder.aab',
        'apk_app': 'Thomas/PipelineFiles/Apps/TimeCard.apk',
        'keystore_file': 'Thomas/PipelineFiles/appdome.keystore',
        'ipa_app_1': 'Thomas/PipelineFiles/Apps/FileFinder.ipa',
        'ipa_app_2': 'Thomas/PipelineFiles/Apps/Trends256-iOS16.ipa',
        'certificate_file': 'Thomas/PipelineFiles/AutomationCert.p12',
        'ipa_1_mobile_provisioning': 'Thomas/PipelineFiles/Automation.mobileprovision',
        'ipa_1_entitlements': 'Thomas/PipelineFiles/AutomationEntitlements.plist',
        'ipa_2_mobile_provisioning_1': 'Thomas/PipelineFiles/Trendsappstoredist.mobileprovision',
        'ipa_2_mobile_provisioning_2': 'Thomas/PipelineFiles/Trends_watchkit_appstoredist.mobileprovision',
        'ipa_2_mobile_provisioning_3': 'Thomas/PipelineFiles/Trends_watchkit_extension_appstoredist.mobileprovision',
        'ipa_2_entitlements_1': 'Thomas/PipelineFiles/main.plist',
        'ipa_2_entitlements_2': 'Thomas/PipelineFiles/watchkit.plist',
        'ipa_2_entitlements_3': 'Thomas/PipelineFiles/watchkitextension.plist',
    }

    if aws_access_key_id is None or aws_secret_access_key is None:
        print("Missing required environment variables.")
        exit(1)

    s3 = boto3.client('s3', aws_access_key_id=aws_access_key_id, aws_secret_access_key=aws_secret_access_key,
                      region_name=aws_default_region)

    presigned_urls = {}

    for key, object_key in objects.items():
        presigned_url = s3.generate_presigned_url(
            'get_object',
            Params={
                'Bucket': bucket_name,
                'Key': object_key
            },
            ExpiresIn=3600  # 1 hour
        )
        presigned_urls[key] = presigned_url

    destination_folder = "downloaded_files"

    # Create destination folder if it doesn't exist
    os.makedirs(destination_folder, exist_ok=True)

    for filename, url in presigned_urls.items():
        download_file(url, destination_folder)


def download_file(url, destination_folder):
    response = requests.get(url)
    if response.status_code == 200:
        filename = url.split("/")[-1]  # Extracts filename from URL
        filepath = os.path.join(destination_folder, filename)
        with open(filepath, 'wb') as file:
            file.write(response.content)
    else:
        print(f"Error downloading {url}: Status Code {response.status_code}")

if __name__ == "__main__":
    main()
