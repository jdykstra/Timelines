project('timelines', version: '1.6', meson_version: '>=0.63.0')

srcdir = meson.current_source_dir() / 'Source'
builddir = meson.current_build_dir()

build_script = meson.current_source_dir() / 'build_jar.sh'

custom_target(
  'timelines-jar',
  output: 'Timelines.jar',
  command: [
    'sh',
    build_script,
    srcdir,
    builddir,
    'Timelines.jar',
    'Timelines',
  ],
  build_by_default: true,
)
