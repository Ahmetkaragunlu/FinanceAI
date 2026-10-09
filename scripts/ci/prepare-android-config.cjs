'use strict';

const fs = require('node:fs');
const path = require('node:path');

/** Build-only placeholders. They are neither usable Firebase credentials nor a release config. */
function prepareAndroidConfig(workspace, environment = process.env) {
    if (environment.GITHUB_ACTIONS !== 'true') throw new Error('SYNTHETIC_CONFIG_IS_CI_ONLY');
    const root = path.resolve(workspace);
    const target = path.join(root, 'app', 'google-services.json');
    if (!fs.existsSync(path.join(root, 'app', 'build.gradle.kts'))) throw new Error('ANDROID_WORKSPACE_REQUIRED');
    if (fs.existsSync(target) || fs.existsSync(path.join(root, 'local.properties')))
        throw new Error('EXISTING_LOCAL_CONFIG_MUST_NOT_BE_REPLACED');
    const config = {
        project_info: { project_number: '1234567890', project_id: 'demo-financeai-ci',
            storage_bucket: 'demo-financeai-ci.firebasestorage.app' },
        client: [{
            client_info: { mobilesdk_app_id: '1:1234567890:android:0123456789abcdef',
                android_client_info: { package_name: 'com.ahmetkaragunlu.financeai' } },
            oauth_client: [{ client_id: '1234567890-ci.apps.googleusercontent.com', client_type: 3 }],
            api_key: [{ current_key: 'ci-firebase-placeholder' }],
            services: { appinvite_service: { other_platform_oauth_client: [] } }
        }],
        configuration_version: '1'
    };
    // Exclusive creation protects even an existing/symlinked target; nothing is printed or read.
    fs.writeFileSync(target, JSON.stringify(config, null, 2) + '\n', { flag: 'wx', mode: 0o600 });
    return target;
}

if (require.main === module) prepareAndroidConfig(process.cwd());
module.exports = { prepareAndroidConfig };
